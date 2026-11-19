package co.crediflow.db.plsql;

import co.crediflow.db.model.TipoProducto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumenEstadisticas(
        long total,
        long aprobadas,
        long rechazadas,
        long enProceso,
        long errores,
        BigDecimal montoAprobado,
        int scorePromedio,
        List<PorProducto> porProducto,
        List<PorDia> ultimosDias) {

    public record PorProducto(TipoProducto producto, long total, long aprobadas, BigDecimal montoAprobado) {
    }

    public record PorDia(LocalDate dia, long total, long aprobadas) {
    }
}
