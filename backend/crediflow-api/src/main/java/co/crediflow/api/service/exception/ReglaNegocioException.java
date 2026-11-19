package co.crediflow.api.service.exception;

/** Operación válida en forma pero no permitida por el estado actual (HTTP 409). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
