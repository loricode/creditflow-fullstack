package co.crediflow.db.plsql;

import co.crediflow.db.model.TipoProducto;

import java.math.BigDecimal;

public record Simulacion(
        TipoProducto producto,
        BigDecimal monto,
        int plazoMeses,
        BigDecimal tasaAnual,
        BigDecimal cuotaMensual,
        BigDecimal totalPagar,
        BigDecimal totalIntereses) {
}
