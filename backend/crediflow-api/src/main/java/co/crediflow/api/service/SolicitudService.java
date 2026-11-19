package co.crediflow.api.service;

import co.crediflow.api.service.exception.RecursoNoEncontradoException;
import co.crediflow.api.service.exception.ReglaNegocioException;
import co.crediflow.db.model.OutboxEvento;
import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.repository.OutboxRepository;
import co.crediflow.db.repository.SolicitudRepository;
import co.crediflow.shared.events.EventosJson;
import co.crediflow.shared.events.SolicitudRecibidaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Casos de uso de escritura. Cada uno guarda el cambio y su evento en la misma
 * transacción (outbox): la API nunca habla con SNS directamente.
 */
@Service
public class SolicitudService {

    private static final Logger log = LoggerFactory.getLogger(SolicitudService.class);

    private final SolicitudRepository solicitudes;
    private final OutboxRepository outbox;

    public SolicitudService(SolicitudRepository solicitudes, OutboxRepository outbox) {
        this.solicitudes = solicitudes;
        this.outbox = outbox;
    }

    /**
     * Registra la solicitud. Idempotente por externalId: si ya existe, la
     * devuelve sin crear otra ni publicar un segundo evento.
     */
    @Transactional
    public ResultadoRegistro registrar(SolicitudCredito nueva) {
        if (nueva.getExternalId() == null || nueva.getExternalId().isBlank()) {
            nueva.setExternalId(UUID.randomUUID().toString());
        } else {
            Optional<SolicitudCredito> existente = solicitudes.findByExternalId(nueva.getExternalId());
            if (existente.isPresent()) {
                log.info("Solicitud repetida externalId={} -> id={}", nueva.getExternalId(), existente.get().getId());
                return new ResultadoRegistro(existente.get(), false);
            }
        }

        SolicitudCredito guardada = solicitudes.saveAndFlush(nueva);
        encolar(guardada);
        log.info("Solicitud registrada id={} producto={} monto={}", guardada.getId(), guardada.getTipoProducto(), guardada.getMonto());
        return new ResultadoRegistro(guardada, true);
    }

    /** Devuelve a la cola una solicitud que agotó sus reintentos. */
    @Transactional
    public SolicitudCredito reintentar(Long id) {
        SolicitudCredito solicitud = solicitudes.findById(id).orElseThrow(() -> RecursoNoEncontradoException.solicitud(id));
        try {
            solicitud.reiniciar();
        } catch (IllegalStateException e) {
            throw new ReglaNegocioException(e.getMessage());
        }
        encolar(solicitud);
        log.info("Solicitud id={} reenviada a evaluación", id);
        return solicitud;
    }

    private void encolar(SolicitudCredito solicitud) {
        SolicitudRecibidaEvent evento = new SolicitudRecibidaEvent(solicitud.getId(), solicitud.getExternalId(), Instant.now());
        outbox.save(new OutboxEvento(solicitud.getId(), SolicitudRecibidaEvent.TIPO, EventosJson.escribir(evento)));
    }

    public record ResultadoRegistro(SolicitudCredito solicitud, boolean nueva) {
    }
}
