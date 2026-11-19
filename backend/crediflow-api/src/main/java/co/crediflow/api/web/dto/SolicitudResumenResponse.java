package co.crediflow.api.web.dto;

import co.crediflow.db.model.EstadoSolicitud;
import co.crediflow.db.model.NivelRiesgo;
import co.crediflow.db.model.TipoProducto;

import java.math.BigDecimal;
import java.time.Instant;

public record SolicitudResumenResponse(
        Long id,
        String externalId,
        String nombreCompleto,
        String numeroDocumento,
        TipoProducto tipoProducto,
        BigDecimal monto,
        Integer plazoMeses,
        EstadoSolicitud estado,
        Integer score,
        NivelRiesgo nivelRiesgo,
        Instant creadoEn) {
}
