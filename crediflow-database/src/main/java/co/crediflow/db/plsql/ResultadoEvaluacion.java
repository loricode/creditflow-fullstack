package co.crediflow.db.plsql;

import co.crediflow.db.model.EstadoSolicitud;
import co.crediflow.db.model.NivelRiesgo;

import java.math.BigDecimal;

public record ResultadoEvaluacion(
        EstadoSolicitud decision,
        int score,
        NivelRiesgo nivelRiesgo,
        BigDecimal tasaAnual,
        BigDecimal cuotaMensual) {

    public boolean aprobada() {
        return decision == EstadoSolicitud.APROBADA;
    }
}
