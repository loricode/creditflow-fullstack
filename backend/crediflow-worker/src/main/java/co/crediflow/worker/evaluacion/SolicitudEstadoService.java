package co.crediflow.worker.evaluacion;

import co.crediflow.db.model.IntentoEvaluacion;
import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.plsql.CreditoPlsqlGateway;
import co.crediflow.db.plsql.ResultadoEvaluacion;
import co.crediflow.db.repository.IntentoRepository;
import co.crediflow.db.repository.SolicitudRepository;
import co.crediflow.worker.ia.Explicacion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Transacciones cortas del flujo de evaluación. Las llamadas externas (S3, IA,
 * SOAP) quedan fuera de cualquier transacción para no retener bloqueos en Oracle.
 */
@Service
public class SolicitudEstadoService {

    private final SolicitudRepository solicitudes;
    private final IntentoRepository intentos;
    private final CreditoPlsqlGateway plsql;
    private final Duration colgada;

    public SolicitudEstadoService(SolicitudRepository solicitudes, IntentoRepository intentos, CreditoPlsqlGateway plsql,
                                  @Value("${crediflow.worker.evaluacion-colgada-segundos:120}") long colgadaSegundos) {
        this.solicitudes = solicitudes;
        this.intentos = intentos;
        this.plsql = plsql;
        this.colgada = Duration.ofSeconds(colgadaSegundos);
    }

    /**
     * Toma la solicitud con bloqueo de fila y la pasa a EN_EVALUACION. Vacío si
     * no existe, ya está decidida, en ERROR o la evalúa otro worker: el mensaje
     * se descarta (idempotencia ante entregas duplicadas de SQS).
     */
    @Transactional
    public Optional<SolicitudCredito> iniciar(Long id) {
        Instant limiteColgada = Instant.now().minus(colgada);
        return solicitudes.bloquear(id)
                .filter(s -> s.disponibleParaEvaluar(limiteColgada))
                .map(s -> {
                    s.iniciarEvaluacion();
                    return solicitudes.saveAndFlush(s);
                });
    }

    /** Ejecuta PKG_CREDITO.EVALUAR_SOLICITUD y guarda la ubicación del original en S3. */
    @Transactional
    public ResultadoEvaluacion evaluar(Long id, String s3Key) {
        try {
            ResultadoEvaluacion resultado = plsql.evaluar(id);
            SolicitudCredito s = solicitudes.findById(id).orElseThrow();
            s.setS3Key(s3Key);
            return resultado;
        } catch (DataAccessException e) {
            if (esErrorDeNegocioPlsql(e)) {
                throw new NoRecuperableException(e.getMostSpecificCause().getMessage(), e);
            }
            throw e;
        }
    }

    @Transactional
    public void finalizar(Long id, ResultadoEvaluacion resultado, Explicacion explicacion, String referenciaCore, long duracionMs) {
        SolicitudCredito s = solicitudes.findById(id).orElseThrow();
        s.finalizar(resultado.decision(), explicacion.texto(), explicacion.proveedor(), referenciaCore);
        intentos.save(IntentoEvaluacion.exito(id, s.getIntentos(), duracionMs));
    }

    /**
     * Registra el intento fallido en una transacción propia (REQUIRES_NEW) para
     * que quede guardado aunque el resto del flujo se revierta.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(Long id, String error, long duracionMs, boolean definitivo) {
        solicitudes.findById(id).ifPresent(s -> {
            intentos.save(IntentoEvaluacion.fallo(id, s.getIntentos(), error, duracionMs));
            if (definitivo) {
                s.marcarError(error);
            } else {
                s.reintentoPendiente(error);
            }
        });
    }

    /** ORA-20001..20999: errores lanzados a propósito por RAISE_APPLICATION_ERROR. */
    static boolean esErrorDeNegocioPlsql(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql && sql.getErrorCode() >= 20000 && sql.getErrorCode() <= 20999) {
                return true;
            }
        }
        return false;
    }
}
