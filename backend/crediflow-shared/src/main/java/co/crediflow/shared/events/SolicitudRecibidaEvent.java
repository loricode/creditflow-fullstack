package co.crediflow.shared.events;

import java.time.Instant;

/**
 * Evento publicado en SNS cuando se registra (o se reintenta) una solicitud.
 * Lleva solo identificadores: el worker lee el estado vigente de la base, así
 * un mensaje viejo o duplicado nunca pisa datos más recientes.
 */
public record SolicitudRecibidaEvent(Long solicitudId, String externalId, Instant ocurridoEn) {

    public static final String TIPO = "SolicitudRecibida";
}
