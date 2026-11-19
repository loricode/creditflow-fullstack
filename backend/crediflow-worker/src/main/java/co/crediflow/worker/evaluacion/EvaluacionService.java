package co.crediflow.worker.evaluacion;

import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.plsql.ResultadoEvaluacion;
import co.crediflow.worker.config.WorkerProperties;
import co.crediflow.worker.ia.Explicacion;
import co.crediflow.worker.ia.ExplicacionIaService;
import co.crediflow.worker.integracion.ArchivoOriginalService;
import co.crediflow.worker.integracion.CoreBancarioClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Flujo de evaluación de una solicitud:
 * <ol>
 *   <li>EN_EVALUACION (cuenta un intento)</li>
 *   <li>Archivar el original en S3</li>
 *   <li>PKG_CREDITO: score, tasa, cuota y amortización</li>
 *   <li>Explicación con IA (con respaldo por reglas)</li>
 *   <li>Si se aprueba: registro en el core bancario por SOAP</li>
 *   <li>Estado final APROBADA / RECHAZADA</li>
 * </ol>
 * Cada paso es idempotente, así que un reintento puede repetir el flujo completo.
 */
@Service
public class EvaluacionService {

    private static final Logger log = LoggerFactory.getLogger(EvaluacionService.class);

    private final SolicitudEstadoService estado;
    private final ArchivoOriginalService archivo;
    private final ExplicacionIaService ia;
    private final CoreBancarioClient core;
    private final WorkerProperties props;

    public EvaluacionService(SolicitudEstadoService estado, ArchivoOriginalService archivo, ExplicacionIaService ia,
                             CoreBancarioClient core, WorkerProperties props) {
        this.estado = estado;
        this.archivo = archivo;
        this.ia = ia;
        this.core = core;
        this.props = props;
    }

    /**
     * @param intentoSqs número de entrega del mensaje (ApproximateReceiveCount)
     * @throws EvaluacionFallidaException si el fallo es recuperable y el mensaje debe reintentarse
     */
    public void procesar(Long solicitudId, int intentoSqs) {
        Optional<SolicitudCredito> enEvaluacion = estado.iniciar(solicitudId);
        if (enEvaluacion.isEmpty()) {
            log.info("Solicitud {} sin trabajo pendiente: mensaje descartado", solicitudId);
            return;
        }
        SolicitudCredito solicitud = enEvaluacion.get();
        long inicio = System.currentTimeMillis();

        try {
            String s3Key = archivo.archivar(solicitud);
            ResultadoEvaluacion resultado = estado.evaluar(solicitudId, s3Key);
            Explicacion explicacion = ia.explicar(solicitud, resultado);
            String referencia = resultado.aprobada() ? core.registrar(solicitud, resultado) : null;

            estado.finalizar(solicitudId, resultado, explicacion, referencia, System.currentTimeMillis() - inicio);
            log.info("Solicitud {} {} score={} explicación={}", solicitudId, resultado.decision(), resultado.score(), explicacion.proveedor());
        } catch (NoRecuperableException e) {
            log.error("Solicitud {} con error no recuperable: {}", solicitudId, e.getMessage());
            registrarFallo(solicitudId, e.getMessage(), inicio, true);
        } catch (RuntimeException e) {
            boolean definitivo = intentoSqs >= props.maxIntentos();
            String error = e.getClass().getSimpleName() + ": " + e.getMessage();
            log.warn("Solicitud {} falló en el intento {}/{}: {}", solicitudId, intentoSqs, props.maxIntentos(), error);
            registrarFallo(solicitudId, error, inicio, definitivo);
            // Sin borrar el mensaje: SQS lo reentrega o, tras maxReceiveCount, lo mueve a la DLQ
            throw new EvaluacionFallidaException(error, e);
        }
    }

    /** Si registrar el fallo también falla (p. ej. base caída) se registra en log y no oculta el error original. */
    private void registrarFallo(Long solicitudId, String error, long inicio, boolean definitivo) {
        try {
            estado.registrarFallo(solicitudId, error, System.currentTimeMillis() - inicio, definitivo);
        } catch (RuntimeException e) {
            log.error("No se pudo registrar el fallo de la solicitud {}: {}", solicitudId, e.getMessage());
        }
    }
}
