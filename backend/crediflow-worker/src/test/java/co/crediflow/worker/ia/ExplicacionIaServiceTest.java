package co.crediflow.worker.ia;

import co.crediflow.db.model.EstadoSolicitud;
import co.crediflow.db.model.NivelRiesgo;
import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.model.TipoProducto;
import co.crediflow.db.plsql.ResultadoEvaluacion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExplicacionIaServiceTest {

    private final ExplicadorReglas reglas = new ExplicadorReglas();

    private static SolicitudCredito solicitud(String ingresos, String gastos) {
        SolicitudCredito s = new SolicitudCredito();
        s.setId(1L);
        s.setTipoProducto(TipoProducto.LIBRE_INVERSION);
        s.setMonto(new BigDecimal("30000000"));
        s.setPlazoMeses(48);
        s.setIngresosMensuales(new BigDecimal(ingresos));
        s.setGastosMensuales(new BigDecimal(gastos));
        return s;
    }

    private static ResultadoEvaluacion resultado(EstadoSolicitud decision, int score, String cuota) {
        return new ResultadoEvaluacion(decision, score, score >= 750 ? NivelRiesgo.BAJO : NivelRiesgo.ALTO,
                new BigDecimal("22.9"), new BigDecimal(cuota));
    }

    @Test
    void sinClaveUsaReglas() {
        ExplicacionIaService servicio = new ExplicacionIaService(reglas, Optional.empty());

        Explicacion e = servicio.explicar(solicitud("8000000", "3000000"), resultado(EstadoSolicitud.APROBADA, 850, "926000"));

        assertThat(e.proveedor()).isEqualTo("reglas");
        assertThat(e.texto()).contains("aprobada").contains("850");
    }

    @Test
    void siClaudeFallaCaeAReglas() {
        ExplicadorClaude claude = mock(ExplicadorClaude.class);
        when(claude.explicar(any())).thenThrow(new RuntimeException("429 rate limit"));
        ExplicacionIaService servicio = new ExplicacionIaService(reglas, Optional.of(claude));

        Explicacion e = servicio.explicar(solicitud("8000000", "3000000"), resultado(EstadoSolicitud.APROBADA, 850, "926000"));

        assertThat(e.proveedor()).isEqualTo("reglas");
    }

    @Test
    void siClaudeSeNiegaCaeAReglas() {
        ExplicadorClaude claude = mock(ExplicadorClaude.class);
        when(claude.explicar(any())).thenReturn(Optional.empty());
        ExplicacionIaService servicio = new ExplicacionIaService(reglas, Optional.of(claude));

        assertThat(servicio.explicar(solicitud("8000000", "3000000"),
                resultado(EstadoSolicitud.APROBADA, 850, "926000")).proveedor()).isEqualTo("reglas");
    }

    @Test
    void usaLaRespuestaDeClaudeCuandoExiste() {
        ExplicadorClaude claude = mock(ExplicadorClaude.class);
        when(claude.explicar(any())).thenReturn(Optional.of(new Explicacion("Tu crédito fue aprobado...", "claude")));
        ExplicacionIaService servicio = new ExplicacionIaService(reglas, Optional.of(claude));

        assertThat(servicio.explicar(solicitud("8000000", "3000000"),
                resultado(EstadoSolicitud.APROBADA, 850, "926000")).proveedor()).isEqualTo("claude");
    }

    @Test
    void rechazoPorCargaExplicaElLimiteDel40() {
        Explicacion e = reglas.explicar(DatosDecision.de(solicitud("3000000", "2500000"),
                resultado(EstadoSolicitud.RECHAZADA, 317, "1900000")));

        assertThat(e.texto()).contains("40 %").contains("monto menor");
    }

    @Test
    void elPromptNoIncluyeDatosPersonales() {
        SolicitudCredito s = solicitud("8000000", "3000000");
        s.setNombreCompleto("Laura Gómez");
        s.setNumeroDocumento("1032456789");
        s.setEmail("laura@correo.co");

        String prompt = ExplicadorClaude.prompt(DatosDecision.de(s, resultado(EstadoSolicitud.APROBADA, 850, "926000")));

        assertThat(prompt).doesNotContain("Laura", "1032456789", "laura@correo.co").contains("APROBADA");
    }
}
