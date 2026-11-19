package co.crediflow.db.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Reglas de transición de estado de la solicitud. */
class SolicitudCreditoTest {

    @Test
    void cadaEntradaAEvaluacionCuentaUnIntento() {
        SolicitudCredito s = new SolicitudCredito();
        s.iniciarEvaluacion();
        s.reintentoPendiente("SOAP Fault");
        s.iniciarEvaluacion();

        assertThat(s.getIntentos()).isEqualTo(2);
        assertThat(s.getEstado()).isEqualTo(EstadoSolicitud.EN_EVALUACION);
        assertThat(s.getUltimoError()).isNull();
    }

    @Test
    void unaSolicitudEnEvaluacionRecienteNoSePuedeTomarDosVeces() {
        SolicitudCredito s = new SolicitudCredito();
        s.setEstado(EstadoSolicitud.RECIBIDA);
        s.alCrear();
        assertThat(s.disponibleParaEvaluar(Instant.now().minusSeconds(120))).isTrue();

        s.iniciarEvaluacion();
        // actualizadoEn es de hace instantes: otro worker la está evaluando
        assertThat(s.disponibleParaEvaluar(Instant.now().minusSeconds(120))).isFalse();
        // si lleva más del umbral, el worker anterior murió: se puede retomar
        assertThat(s.disponibleParaEvaluar(Instant.now().plusSeconds(1))).isTrue();
    }

    @Test
    void lasDecididasYLasDeErrorNoSeVuelvenAEvaluar() {
        SolicitudCredito s = new SolicitudCredito();
        s.finalizar(EstadoSolicitud.APROBADA, "ok", "reglas", "CB-1");
        assertThat(s.disponibleParaEvaluar(Instant.now().plusSeconds(999))).isFalse();

        s.marcarError("x");
        assertThat(s.disponibleParaEvaluar(Instant.now().plusSeconds(999))).isFalse();
    }

    @Test
    void soloSeReintentaDesdeError() {
        SolicitudCredito s = new SolicitudCredito();
        s.setEstado(EstadoSolicitud.RECHAZADA);
        assertThatThrownBy(s::reiniciar).isInstanceOf(IllegalStateException.class);

        s.marcarError("x");
        s.reiniciar();
        assertThat(s.getEstado()).isEqualTo(EstadoSolicitud.RECIBIDA);
    }

    @Test
    void finalizarExigeUnaDecisionFinal() {
        assertThatThrownBy(() -> new SolicitudCredito().finalizar(EstadoSolicitud.ERROR, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
