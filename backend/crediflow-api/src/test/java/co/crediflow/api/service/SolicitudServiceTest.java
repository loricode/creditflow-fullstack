package co.crediflow.api.service;

import co.crediflow.api.service.exception.ReglaNegocioException;
import co.crediflow.db.model.EstadoSolicitud;
import co.crediflow.db.model.OutboxEvento;
import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.repository.OutboxRepository;
import co.crediflow.db.repository.SolicitudRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SolicitudServiceTest {

    private final SolicitudRepository solicitudes = mock(SolicitudRepository.class);
    private final OutboxRepository outbox = mock(OutboxRepository.class);
    private final SolicitudService servicio = new SolicitudService(solicitudes, outbox);

    private static SolicitudCredito nueva(String externalId) {
        SolicitudCredito s = new SolicitudCredito();
        s.setExternalId(externalId);
        return s;
    }

    @Test
    void registraLaSolicitudYSuEventoEnElOutbox() {
        when(solicitudes.findByExternalId("abc")).thenReturn(Optional.empty());
        when(solicitudes.saveAndFlush(any())).thenAnswer(inv -> {
            SolicitudCredito s = inv.getArgument(0);
            s.setId(42L);
            return s;
        });

        SolicitudService.ResultadoRegistro r = servicio.registrar(nueva("abc"));

        assertThat(r.nueva()).isTrue();
        ArgumentCaptor<OutboxEvento> evento = ArgumentCaptor.forClass(OutboxEvento.class);
        verify(outbox).save(evento.capture());
        assertThat(evento.getValue().getAgregadoId()).isEqualTo(42L);
        assertThat(evento.getValue().getTipo()).isEqualTo("SolicitudRecibida");
        assertThat(evento.getValue().getPayload()).contains("\"solicitudId\":42").contains("\"externalId\":\"abc\"");
    }

    @Test
    void esIdempotentePorExternalId() {
        SolicitudCredito existente = nueva("abc");
        existente.setId(7L);
        when(solicitudes.findByExternalId("abc")).thenReturn(Optional.of(existente));

        SolicitudService.ResultadoRegistro r = servicio.registrar(nueva("abc"));

        assertThat(r.nueva()).isFalse();
        assertThat(r.solicitud().getId()).isEqualTo(7L);
        verify(solicitudes, never()).saveAndFlush(any());
        verify(outbox, never()).save(any());
    }

    @Test
    void generaExternalIdSiNoLlega() {
        when(solicitudes.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        SolicitudService.ResultadoRegistro r = servicio.registrar(nueva(null));

        assertThat(r.solicitud().getExternalId()).hasSize(36);
    }

    @Test
    void reintentarSoloDesdeError() {
        SolicitudCredito aprobada = nueva("x");
        aprobada.setId(1L);
        aprobada.setEstado(EstadoSolicitud.APROBADA);
        when(solicitudes.findById(1L)).thenReturn(Optional.of(aprobada));

        assertThatThrownBy(() -> servicio.reintentar(1L)).isInstanceOf(ReglaNegocioException.class);
        verify(outbox, never()).save(any());
    }

    @Test
    void reintentarDevuelveALaColaYPublicaEvento() {
        SolicitudCredito conError = nueva("x");
        conError.setId(1L);
        conError.marcarError("SOAP Fault");
        when(solicitudes.findById(1L)).thenReturn(Optional.of(conError));

        SolicitudCredito r = servicio.reintentar(1L);

        assertThat(r.getEstado()).isEqualTo(EstadoSolicitud.RECIBIDA);
        assertThat(r.getUltimoError()).isNull();
        verify(outbox).save(any());
    }
}
