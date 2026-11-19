package co.crediflow.db.model;

public enum EstadoSolicitud {
    RECIBIDA, EN_EVALUACION, APROBADA, RECHAZADA, ERROR;

    public boolean esFinal() {
        return this == APROBADA || this == RECHAZADA;
    }
}
