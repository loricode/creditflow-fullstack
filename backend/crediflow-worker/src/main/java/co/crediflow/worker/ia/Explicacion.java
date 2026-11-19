package co.crediflow.worker.ia;

/**
 * @param texto     explicación en lenguaje claro para el solicitante
 * @param proveedor quién la generó: "claude" o "reglas"
 */
public record Explicacion(String texto, String proveedor) {
}
