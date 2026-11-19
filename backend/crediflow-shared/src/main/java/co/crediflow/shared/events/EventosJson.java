package co.crediflow.shared.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/** Serialización de eventos con un formato fijo, independiente del ObjectMapper de cada servicio. */
public final class EventosJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private EventosJson() {
    }

    public static String escribir(Object evento) {
        try {
            return MAPPER.writeValueAsString(evento);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Evento no serializable: " + evento, e);
        }
    }

    public static <T> T leer(String json, Class<T> tipo) {
        try {
            return MAPPER.readValue(json, tipo);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Mensaje con formato inválido para " + tipo.getSimpleName(), e);
        }
    }
}
