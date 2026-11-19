package co.crediflow.api.web.dto;

import co.crediflow.db.model.EstadoSolicitud;
import co.crediflow.db.model.NivelRiesgo;
import co.crediflow.db.model.ResultadoIntento;
import co.crediflow.db.model.TipoProducto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SolicitudDetalleResponse(
        Long id,
        String externalId,
        String numeroDocumento,
        String nombreCompleto,
        String email,
        BigDecimal ingresosMensuales,
        BigDecimal gastosMensuales,
        TipoProducto tipoProducto,
        BigDecimal monto,
        Integer plazoMeses,
        EstadoSolicitud estado,
        Integer score,
        NivelRiesgo nivelRiesgo,
        BigDecimal tasaAnual,
        BigDecimal cuotaMensual,
        String explicacionIa,
        String proveedorIa,
        String referenciaCore,
        String s3Key,
        String ultimoError,
        Integer intentos,
        Instant creadoEn,
        Instant actualizadoEn,
        Instant evaluadoEn,
        List<Historial> historial,
        List<Intento> historialIntentos) {

    public record Historial(EstadoSolicitud estadoAnterior, EstadoSolicitud estadoNuevo, Instant cambiadoEn) {
    }

    public record Intento(Integer numeroIntento, ResultadoIntento resultado, String error, Long duracionMs, Instant creadoEn) {
    }
}
