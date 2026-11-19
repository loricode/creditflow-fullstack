package co.crediflow.api.web.dto;

import co.crediflow.db.model.TipoProducto;

import java.math.BigDecimal;

public record SimulacionResponse(
        TipoProducto producto,
        BigDecimal monto,
        int plazoMeses,
        BigDecimal tasaAnual,
        BigDecimal cuotaMensual,
        BigDecimal totalPagar,
        BigDecimal totalIntereses) {
}
