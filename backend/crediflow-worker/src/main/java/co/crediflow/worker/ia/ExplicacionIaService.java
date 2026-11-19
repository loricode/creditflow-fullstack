package co.crediflow.worker.ia;

import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.plsql.ResultadoEvaluacion;
import co.crediflow.worker.config.IaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * La IA aporta valor pero no es crítica: cualquier fallo (sin clave, timeout,
 * límite de uso, negativa del modelo) cae al explicador por reglas y la
 * evaluación sigue. Nunca provoca un reintento del mensaje.
 */
@Service
public class ExplicacionIaService {

    private static final Logger log = LoggerFactory.getLogger(ExplicacionIaService.class);

    private final ExplicadorReglas reglas;
    private final Optional<ExplicadorClaude> claude;

    @Autowired
    public ExplicacionIaService(ExplicadorReglas reglas, IaProperties props) {
        this(reglas, props.habilitada() ? Optional.of(new ExplicadorClaude(props)) : Optional.empty());
        if (!props.habilitada()) {
            log.info("IA sin clave configurada: las explicaciones se generan por reglas");
        }
    }

    ExplicacionIaService(ExplicadorReglas reglas, Optional<ExplicadorClaude> claude) {
        this.reglas = reglas;
        this.claude = claude;
    }

    public Explicacion explicar(SolicitudCredito solicitud, ResultadoEvaluacion resultado) {
        DatosDecision datos = DatosDecision.de(solicitud, resultado);
        if (claude.isPresent()) {
            try {
                Optional<Explicacion> generada = claude.get().explicar(datos);
                if (generada.isPresent()) {
                    return generada.get();
                }
                log.warn("Claude no devolvió explicación para la solicitud {}; se usan reglas", solicitud.getId());
            } catch (RuntimeException e) {
                log.warn("Fallo de IA en la solicitud {} ({}); se usan reglas", solicitud.getId(), e.getMessage());
            }
        }
        return reglas.explicar(datos);
    }
}
