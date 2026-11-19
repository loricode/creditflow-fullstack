package co.crediflow.publisher;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param topicArn    ARN del tópico SNS de solicitudes recibidas
 * @param lote        eventos por ciclo
 * @param maxIntentos fallos de publicación antes de marcar el evento FALLIDO
 */
@ConfigurationProperties(prefix = "crediflow.publisher")
public record PublisherProperties(String topicArn, int lote, int maxIntentos) {

    public PublisherProperties {
        if (topicArn == null || topicArn.isBlank()) {
            throw new IllegalArgumentException("crediflow.publisher.topic-arn es obligatorio");
        }
        lote = lote <= 0 ? 20 : lote;
        maxIntentos = maxIntentos <= 0 ? 5 : maxIntentos;
    }
}
