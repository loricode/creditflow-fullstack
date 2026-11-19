package co.crediflow.publisher;

import co.crediflow.db.model.OutboxEvento;
import co.crediflow.db.repository.OutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;

import java.util.List;
import java.util.Map;

/**
 * Lee el outbox y publica en SNS. Entrega al-menos-una-vez: si el proceso cae
 * después de publicar y antes del COMMIT, el evento se publica de nuevo y el
 * worker lo descarta por idempotencia.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxRepository outbox;
    private final SnsClient sns;
    private final PublisherProperties props;
    private final TransactionTemplate transaccion;

    public OutboxPublisher(OutboxRepository outbox, SnsClient sns, PublisherProperties props,
                           PlatformTransactionManager transactionManager) {
        this.outbox = outbox;
        this.sns = sns;
        this.props = props;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    @Scheduled(fixedDelayString = "${crediflow.publisher.intervalo-ms:1000}")
    public void ciclo() {
        try {
            // Transacción explícita: una llamada interna a un método @Transactional
            // no pasa por el proxy de Spring y se ejecutaría sin transacción.
            Integer publicados = transaccion.execute(estado -> publicarLote());
            if (publicados != null && publicados > 0) {
                log.info("Publicados {} eventos en SNS", publicados);
            }
        } catch (Exception e) {
            // Base de datos caída u otro error transitorio: se reintenta en el próximo ciclo
            log.warn("Ciclo del publicador fallido: {}", e.getMessage());
        }
    }

    /** Debe ejecutarse dentro de una transacción (el bloqueo FOR UPDATE dura hasta el COMMIT). */
    int publicarLote() {
        List<OutboxEvento> pendientes = outbox.bloquearPendientes(props.lote());
        int publicados = 0;
        for (OutboxEvento evento : pendientes) {
            try {
                sns.publish(r -> r.topicArn(props.topicArn())
                        .message(evento.getPayload())
                        .messageAttributes(Map.of(
                                "tipo", texto(evento.getTipo()),
                                "solicitudId", texto(String.valueOf(evento.getAgregadoId())))));
                evento.marcarPublicado();
                publicados++;
            } catch (RuntimeException e) {
                evento.registrarFallo(e.getMessage(), props.maxIntentos());
                log.warn("No se pudo publicar el evento {} (intento {}): {}", evento.getId(), evento.getIntentos(), e.getMessage());
            }
        }
        return publicados;
    }

    private static MessageAttributeValue texto(String valor) {
        return MessageAttributeValue.builder().dataType("String").stringValue(valor).build();
    }
}
