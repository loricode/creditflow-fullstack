package co.crediflow.api.web.dto;

import co.crediflow.db.model.TipoProducto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CrearSolicitudRequest(
        @Schema(description = "Clave de idempotencia generada por el cliente. Reenviar la misma clave devuelve la solicitud ya creada.",
                example = "7f3c2a1e-5b6d-4c8e-9f00-112233445566")
        @Size(max = 64) String externalId,

        @Schema(example = "1032456789")
        @NotBlank @Pattern(regexp = "\\d{6,12}", message = "debe tener entre 6 y 12 dígitos") String numeroDocumento,

        @Schema(example = "Laura Gómez Rincón")
        @NotBlank @Size(max = 150) String nombreCompleto,

        @Schema(example = "laura.gomez@correo.co")
        @NotBlank @Email @Size(max = 150) String email,

        @Schema(example = "8500000")
        @NotNull @DecimalMin(value = "500000", message = "debe ser al menos 500.000") BigDecimal ingresosMensuales,

        @Schema(example = "3200000")
        @NotNull @DecimalMin(value = "0") BigDecimal gastosMensuales,

        @NotNull TipoProducto tipoProducto,

        @Schema(example = "35000000")
        @NotNull @DecimalMin(value = "1000000", message = "el monto mínimo es 1.000.000")
        @DecimalMax(value = "2000000000", message = "el monto máximo es 2.000.000.000") BigDecimal monto,

        @Schema(example = "48")
        @NotNull @Min(6) @Max(360) Integer plazoMeses) {
}
