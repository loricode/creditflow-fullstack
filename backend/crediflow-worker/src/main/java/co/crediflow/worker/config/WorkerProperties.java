package co.crediflow.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param queueUrl       URL de la cola SQS de evaluación
 * @param concurrencia   hilos consumidores
 * @param maxIntentos    debe coincidir con maxReceiveCount de la redrive policy (Terraform)
 * @param esperaSegundos long polling de SQS
 * @param bucket         bucket S3 donde se archiva el original
 */
@ConfigurationProperties(prefix = "crediflow.worker")
public record WorkerProperties(String queueUrl, int concurrencia, int maxIntentos, int esperaSegundos, String bucket) {

    public WorkerProperties {
        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalArgumentException("crediflow.worker.queue-url es obligatorio");
        }
        concurrencia = concurrencia <= 0 ? 2 : concurrencia;
        maxIntentos = maxIntentos <= 0 ? 3 : maxIntentos;
        esperaSegundos = esperaSegundos <= 0 ? 10 : Math.min(esperaSegundos, 20);
    }
}
