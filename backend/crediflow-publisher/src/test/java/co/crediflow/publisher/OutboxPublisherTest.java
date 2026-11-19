package co.crediflow.publisher;

import co.crediflow.db.model.EstadoOutbox;
import co.crediflow.db.model.OutboxEvento;
import co.crediflow.db.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxPublisherTest {

    private final OutboxRepository outbox = mock(OutboxRepository.class);
    private final SnsClient sns = mock(SnsClient.class);
    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {
        PublisherProperties props = new PublisherProperties("arn:aws:sns:us-east-1:000000000000:t", 10, 2);
        publisher = new OutboxPublisher(outbox, sns, props, mock(PlatformTransactionManager.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void marcaPublicadoCuandoSnsAcepta() {
        OutboxEvento evento = new OutboxEvento(1L, "SolicitudRecibida", "{\"solicitudId\":1}");
        when(outbox.bloquearPendientes(10)).thenReturn(List.of(evento));
        when(sns.publish(any(Consumer.class))).thenReturn(PublishResponse.builder().messageId("m-1").build());

        int publicados = publisher.publicarLote();

        assertThat(publicados).isEqualTo(1);
        assertThat(evento.getEstado()).isEqualTo(EstadoOutbox.PUBLICADO);
        assertThat(evento.getPublicadoEn()).isNotNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void registraFalloYMarcaFallidoAlAgotarIntentos() {
        OutboxEvento evento = new OutboxEvento(2L, "SolicitudRecibida", "{\"solicitudId\":2}");
        when(outbox.bloquearPendientes(10)).thenReturn(List.of(evento));
        when(sns.publish(any(Consumer.class))).thenThrow(SnsException.builder().message("SNS no disponible").build());

        publisher.publicarLote();
        assertThat(evento.getEstado()).isEqualTo(EstadoOutbox.PENDIENTE);
        assertThat(evento.getIntentos()).isEqualTo(1);

        publisher.publicarLote();
        assertThat(evento.getEstado()).isEqualTo(EstadoOutbox.FALLIDO);
        assertThat(evento.getUltimoError()).contains("SNS no disponible");
    }

    @Test
    void cicloNoPropagaErroresDeBaseDeDatos() {
        when(outbox.bloquearPendientes(10)).thenThrow(new IllegalStateException("ORA-12541"));
        publisher.ciclo(); // no lanza
    }

    @Test
    @SuppressWarnings("unchecked")
    void cicloEjecutaElLoteDentroDeUnaTransaccion() {
        PlatformTransactionManager tm = mock(PlatformTransactionManager.class);
        OutboxPublisher conTx = new OutboxPublisher(outbox, sns,
                new PublisherProperties("arn:aws:sns:us-east-1:000000000000:t", 10, 2), tm);
        when(outbox.bloquearPendientes(10)).thenReturn(List.of());

        conTx.ciclo();

        // Regresión: antes ciclo() llamaba a un @Transactional interno y no abría transacción
        verify(tm).getTransaction(any());
        verify(tm).commit(any());
    }
}
