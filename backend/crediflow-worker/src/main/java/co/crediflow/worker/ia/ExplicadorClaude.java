package co.crediflow.worker.ia;

import co.crediflow.worker.config.IaProperties;
import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlock;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Genera la explicación de la decisión con Claude (SDK oficial anthropic-java).
 * Devuelve vacío si el modelo se niega o no produce texto; el llamador usa
 * entonces el explicador por reglas.
 */
public class ExplicadorClaude {

    static final String PROVEEDOR = "claude";

    private static final String SISTEMA = """
            Eres analista de crédito de un banco colombiano y escribes al solicitante.
            Explica la decisión en español claro, en 3 o 4 frases y en segunda persona (tú), sin tecnicismos.
            Usa solo las cifras que recibes; no inventes datos, condiciones ni promesas.
            Si fue aprobada, menciona la cuota, la tasa y qué parte de su capacidad de pago representa.
            Si fue rechazada, explica el motivo principal y da una o dos acciones concretas para mejorar.
            Formato: texto plano, sin listas, sin markdown y sin saludo ni despedida.
            """;

    private final AnthropicClient client;
    private final String modelo;

    public ExplicadorClaude(IaProperties props) {
        this(AnthropicOkHttpClient.builder()
                .apiKey(props.apiKey())
                .timeout(Duration.ofSeconds(props.timeoutSegundos()))
                .maxRetries(1)
                .build(), props.modelo());
    }

    ExplicadorClaude(AnthropicClient client, String modelo) {
        this.client = client;
        this.modelo = modelo;
    }

    public Optional<Explicacion> explicar(DatosDecision d) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(modelo)
                .maxTokens(1024L)
                .system(SISTEMA)
                // Tarea corta y acotada: esfuerzo bajo = menor latencia y costo
                .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.LOW).build())
                .addUserMessage(prompt(d))
                .build();

        Message respuesta = client.messages().create(params);

        if (respuesta.stopReason().filter(StopReason.REFUSAL::equals).isPresent()) {
            return Optional.empty();
        }
        String texto = respuesta.content().stream()
                .flatMap(bloque -> bloque.text().stream())
                .map(TextBlock::text)
                .collect(Collectors.joining("\n"))
                .trim();
        return texto.isEmpty() ? Optional.empty() : Optional.of(new Explicacion(texto, PROVEEDOR));
    }

    static String prompt(DatosDecision d) {
        return """
                Decisión: %s
                Producto: %s
                Monto solicitado: %s COP a %d meses
                Ingresos mensuales: %s COP
                Gastos mensuales: %s COP (endeudamiento %s %%)
                Puntaje: %d de 1000 (riesgo %s)
                Tasa asignada: %s %% efectivo anual
                Cuota mensual: %s COP (%s %% de la capacidad de pago; el máximo permitido es 40 %%)
                """.formatted(d.decision(), d.producto(), d.monto().toPlainString(), d.plazoMeses(),
                d.ingresosMensuales().toPlainString(), d.gastosMensuales().toPlainString(), d.endeudamiento(),
                d.score(), d.nivelRiesgo(), d.tasaAnual().toPlainString(), d.cuotaMensual().toPlainString(), d.cargaCuota());
    }
}
