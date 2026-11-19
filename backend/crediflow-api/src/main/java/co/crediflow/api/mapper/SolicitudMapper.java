package co.crediflow.api.mapper;

import co.crediflow.api.web.dto.CrearSolicitudRequest;
import co.crediflow.api.web.dto.CuotaResponse;
import co.crediflow.api.web.dto.EstadisticasResponse;
import co.crediflow.api.web.dto.SimulacionResponse;
import co.crediflow.api.web.dto.SolicitudDetalleResponse;
import co.crediflow.api.web.dto.SolicitudResumenResponse;
import co.crediflow.db.model.CuotaAmortizacion;
import co.crediflow.db.model.HistorialEstado;
import co.crediflow.db.model.IntentoEvaluacion;
import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.plsql.ResumenEstadisticas;
import co.crediflow.db.plsql.Simulacion;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/** Traducción entidad ↔ DTO generada en compilación (sin reflexión en tiempo de ejecución). */
@Mapper
public interface SolicitudMapper {

    SolicitudResumenResponse aResumen(SolicitudCredito solicitud);

    SolicitudDetalleResponse aDetalle(SolicitudCredito solicitud,
                                      List<HistorialEstado> historial,
                                      List<IntentoEvaluacion> historialIntentos);

    SolicitudDetalleResponse.Historial aHistorial(HistorialEstado historial);

    SolicitudDetalleResponse.Intento aIntento(IntentoEvaluacion intento);

    CuotaResponse aCuota(CuotaAmortizacion cuota);

    SimulacionResponse aSimulacion(Simulacion simulacion);

    /** Solo los datos del solicitante: el resto (estado, resultado, auditoría) lo controla el sistema. */
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "intentos", ignore = true)
    SolicitudCredito aEntidad(CrearSolicitudRequest request);

    @Mapping(target = "tasaAprobacion", expression = "java(tasaAprobacion(resumen))")
    EstadisticasResponse aEstadisticas(ResumenEstadisticas resumen);

    EstadisticasResponse.PorProducto aPorProducto(ResumenEstadisticas.PorProducto porProducto);

    EstadisticasResponse.PorDia aPorDia(ResumenEstadisticas.PorDia porDia);

    /** Aprobadas sobre decididas (aprobadas + rechazadas), en porcentaje con un decimal. */
    default double tasaAprobacion(ResumenEstadisticas r) {
        long decididas = r.aprobadas() + r.rechazadas();
        return decididas == 0 ? 0.0 : Math.round(r.aprobadas() * 1000.0 / decididas) / 10.0;
    }
}
