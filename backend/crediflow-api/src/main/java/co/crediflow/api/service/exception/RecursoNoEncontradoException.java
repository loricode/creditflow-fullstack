package co.crediflow.api.service.exception;

public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }

    public static RecursoNoEncontradoException solicitud(Long id) {
        return new RecursoNoEncontradoException("No existe la solicitud " + id);
    }
}
