package co.crediflow.api.web.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaResponse(
        Integer numeroCuota,
        LocalDate fechaPago,
        BigDecimal cuota,
        BigDecimal capital,
        BigDecimal interes,
        BigDecimal saldo) {
}
