package co.crediflow.api.web.dto;

import co.crediflow.db.model.TipoProducto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EstadisticasResponse(
        long total,
        long aprobadas,
        long rechazadas,
        long enProceso,
        long errores,
        BigDecimal montoAprobado,
        int scorePromedio,
        double tasaAprobacion,
        List<PorProducto> porProducto,
        List<PorDia> ultimosDias) {

    public record PorProducto(TipoProducto producto, long total, long aprobadas, BigDecimal montoAprobado) {
    }

    public record PorDia(LocalDate dia, long total, long aprobadas) {
    }
}
