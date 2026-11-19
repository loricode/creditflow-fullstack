package co.crediflow.worker.evaluacion;

/** Error que no se arregla reintentando (datos inválidos, regla PL/SQL violada). */
public class NoRecuperableException extends RuntimeException {

    public NoRecuperableException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
