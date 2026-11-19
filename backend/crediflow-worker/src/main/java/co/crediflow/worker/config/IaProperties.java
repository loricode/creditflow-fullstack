package co.crediflow.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param apiKey          clave de la API de Claude (normalmente desde Secrets Manager). Vacía = solo reglas.
 * @param modelo          modelo de Claude
 * @param timeoutSegundos tiempo máximo por llamada; la explicación nunca debe frenar la evaluación
 */
@ConfigurationProperties(prefix = "crediflow.ia")
public record IaProperties(String apiKey, String modelo, int timeoutSegundos) {

    public IaProperties {
        modelo = (modelo == null || modelo.isBlank()) ? "claude-opus-5-5" : modelo;
        timeoutSegundos = timeoutSegundos <= 0 ? 30 : timeoutSegundos;
    }

    public boolean habilitada() {
        return apiKey != null && !apiKey.isBlank();
    }
}
