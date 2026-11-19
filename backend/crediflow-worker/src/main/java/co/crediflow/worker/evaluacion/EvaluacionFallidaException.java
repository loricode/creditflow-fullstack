package co.crediflow.worker.evaluacion;

/** Fallo recuperable: el mensaje no se borra de SQS y vuelve a entregarse tras el visibility timeout. */
public class EvaluacionFallidaException extends RuntimeException {

    public EvaluacionFallidaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
