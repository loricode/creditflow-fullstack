package co.crediflow.worker.ia;

import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.plsql.ResultadoEvaluacion;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Lo único que sale hacia el modelo: cifras de la decisión, sin nombre,
 * documento ni correo del solicitante (minimización de datos personales).
 */
public record DatosDecision(
        String producto,
        BigDecimal monto,
        int plazoMeses,
        BigDecimal ingresosMensuales,
        BigDecimal gastosMensuales,
        String decision,
        int score,
        String nivelRiesgo,
        BigDecimal tasaAnual,
        BigDecimal cuotaMensual) {

    public static DatosDecision de(SolicitudCredito s, ResultadoEvaluacion r) {
        return new DatosDecision(s.getTipoProducto().name(), s.getMonto(), s.getPlazoMeses(),
                s.getIngresosMensuales(), s.getGastosMensuales(), r.decision().name(), r.score(),
                r.nivelRiesgo().name(), r.tasaAnual(), r.cuotaMensual());
    }

    public BigDecimal capacidadPago() {
        return ingresosMensuales.subtract(gastosMensuales);
    }

    /** Porcentaje de la capacidad de pago que consumiría la cuota. */
    public BigDecimal cargaCuota() {
        BigDecimal capacidad = capacidadPago();
        if (capacidad.signum() <= 0) {
            return BigDecimal.valueOf(100);
        }
        return cuotaMensual.multiply(BigDecimal.valueOf(100)).divide(capacidad, 1, RoundingMode.HALF_UP);
    }

    public BigDecimal endeudamiento() {
        return gastosMensuales.multiply(BigDecimal.valueOf(100)).divide(ingresosMensuales, 1, RoundingMode.HALF_UP);
    }
}
