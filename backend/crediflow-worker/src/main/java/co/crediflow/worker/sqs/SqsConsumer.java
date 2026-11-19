package co.crediflow.worker.sqs;

import co.crediflow.shared.events.EventosJson;
import co.crediflow.shared.events.SolicitudRecibidaEvent;
import co.crediflow.worker.config.WorkerProperties;
import co.crediflow.worker.evaluacion.EvaluacionFallidaException;
import co.crediflow.worker.evaluacion.EvaluacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Consumidor de SQS con long polling y N hilos. Borra el mensaje solo cuando
 * el procesamiento termina (éxito o error no recuperable); ante un fallo
 * recuperable lo deja en la cola y SQS lo reentrega tras el visibility timeout.
 */
@Component
public class SqsConsumer implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(SqsConsumer.class);

    private final SqsClient sqs;
    private final EvaluacionService evaluacion;
    private final WorkerProperties props;
    private volatile boolean activo;
    private ExecutorService hilos;

    public SqsConsumer(SqsClient sqs, EvaluacionService evaluacion, WorkerProperties props) {
        this.sqs = sqs;
        this.evaluacion = evaluacion;
        this.props = props;
    }

    @Override
    public void start() {
        activo = true;
        hilos = Executors.newFixedThreadPool(props.concurrencia(), r -> {
            Thread t = new Thread(r);
            t.setName("sqs-consumer-" + t.getId());
            return t;
        });
        for (int i = 0; i < props.concurrencia(); i++) {
            hilos.submit(this::bucle);
        }
        log.info("Consumiendo {} con {} hilos", props.queueUrl(), props.concurrencia());
    }

    private void bucle() {
        while (activo) {
            try {
                List<Message> mensajes = sqs.receiveMessage(r -> r.queueUrl(props.queueUrl())
                        .maxNumberOfMessages(5)
                        .waitTimeSeconds(props.esperaSegundos())
                        .messageSystemAttributeNames(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT)).messages();
                mensajes.forEach(this::procesar);
            } catch (Exception e) {
                if (activo) {
                    log.warn("Error leyendo SQS: {}. Reintento en 5 s", e.getMessage());
                    dormir(5_000);
                }
            }
        }
    }

    void procesar(Message mensaje) {
        int intento = Integer.parseInt(mensaje.attributes().getOrDefault(MessageSystemAttributeName.APPROXIMATE_RECEIVE_COUNT, "1"));
        try {
            SolicitudRecibidaEvent evento = EventosJson.leer(mensaje.body(), SolicitudRecibidaEvent.class);
            evaluacion.procesar(evento.solicitudId(), intento);
            borrar(mensaje);
        } catch (IllegalArgumentException e) {
            // Mensaje mal formado: reintentar no lo arregla
            log.error("Mensaje inválido descartado {}: {}", mensaje.messageId(), e.getMessage());
            borrar(mensaje);
        } catch (EvaluacionFallidaException e) {
            log.debug("Mensaje {} queda en la cola para reintento", mensaje.messageId());
        } catch (RuntimeException e) {
            log.error("Error inesperado con el mensaje {}", mensaje.messageId(), e);
        }
    }

    private void borrar(Message mensaje) {
        sqs.deleteMessage(r -> r.queueUrl(props.queueUrl()).receiptHandle(mensaje.receiptHandle()));
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        activo = false;
        if (hilos != null) {
            hilos.shutdown();
            try {
                // Espera a que terminen los mensajes en curso (cierre ordenado)
                hilos.awaitTermination(props.esperaSegundos() + 15L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return activo;
    }
}
