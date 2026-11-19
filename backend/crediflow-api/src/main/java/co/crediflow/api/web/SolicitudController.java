package co.crediflow.api.web;

import co.crediflow.api.mapper.SolicitudMapper;
import co.crediflow.api.service.ConsultaSolicitudService;
import co.crediflow.api.service.SolicitudService;
import co.crediflow.api.web.dto.CrearSolicitudRequest;
import co.crediflow.api.web.dto.CuotaResponse;
import co.crediflow.api.web.dto.PaginaResponse;
import co.crediflow.api.web.dto.SolicitudDetalleResponse;
import co.crediflow.api.web.dto.SolicitudResumenResponse;
import co.crediflow.db.model.EstadoSolicitud;
import co.crediflow.db.model.TipoProducto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Set;

@RestController
@Validated
@RequestMapping("/api/v1/solicitudes")
@Tag(name = "Solicitudes", description = "Registro y consulta de solicitudes de crédito")
public class SolicitudController {

    private static final Set<String> ORDEN_PERMITIDO = Set.of("creadoEn", "monto", "score", "nombreCompleto", "estado");

    private final SolicitudService solicitudService;
    private final ConsultaSolicitudService consultaService;
    private final SolicitudMapper mapper;

    public SolicitudController(SolicitudService solicitudService, ConsultaSolicitudService consultaService, SolicitudMapper mapper) {
        this.solicitudService = solicitudService;
        this.consultaService = consultaService;
        this.mapper = mapper;
    }

    @PostMapping
    @Operation(summary = "Registrar solicitud",
            description = "201 si es nueva, 200 si la clave externalId ya existía (idempotencia). La evaluación es asíncrona.")
    public ResponseEntity<SolicitudDetalleResponse> registrar(@Valid @RequestBody CrearSolicitudRequest request) {
        SolicitudService.ResultadoRegistro resultado = solicitudService.registrar(mapper.aEntidad(request));
        SolicitudDetalleResponse cuerpo = mapper.aDetalle(resultado.solicitud(), List.of(), List.of());
        if (!resultado.nueva()) {
            return ResponseEntity.ok(cuerpo);
        }
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(resultado.solicitud().getId()).toUri();
        return ResponseEntity.created(ubicacion).body(cuerpo);
    }

    @GetMapping
    @Operation(summary = "Listar solicitudes", description = "Filtros opcionales por estado, producto y texto (nombre, documento o externalId).")
    public PaginaResponse<SolicitudResumenResponse> listar(
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) TipoProducto producto,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int tamano,
            @RequestParam(defaultValue = "creadoEn") String orden,
            @RequestParam(defaultValue = "desc") String direccion) {
        String campo = ORDEN_PERMITIDO.contains(orden) ? orden : "creadoEn";
        Sort sort = Sort.by(Sort.Direction.fromOptionalString(direccion).orElse(Sort.Direction.DESC), campo)
                .and(Sort.by(Sort.Direction.DESC, "id"));
        return PaginaResponse.de(consultaService.buscar(estado, producto, q, PageRequest.of(pagina, tamano, sort)), mapper::aResumen);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle con historial de estados e intentos de evaluación")
    public SolicitudDetalleResponse detalle(@PathVariable Long id) {
        ConsultaSolicitudService.Detalle d = consultaService.detalle(id);
        return mapper.aDetalle(d.solicitud(), d.historial(), d.intentos());
    }

    @GetMapping("/{id}/amortizacion")
    @Operation(summary = "Tabla de amortización (solo solicitudes aprobadas)")
    public List<CuotaResponse> amortizacion(@PathVariable Long id) {
        return consultaService.amortizacion(id).stream().map(mapper::aCuota).toList();
    }

    @GetMapping(value = "/{id}/original", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Solicitud original archivada en S3")
    public String original(@PathVariable Long id) {
        return consultaService.original(id);
    }

    @PostMapping("/{id}/reintentar")
    @Operation(summary = "Reenviar a evaluación una solicitud en ERROR")
    public ResponseEntity<SolicitudResumenResponse> reintentar(@PathVariable Long id) {
        return ResponseEntity.accepted().body(mapper.aResumen(solicitudService.reintentar(id)));
    }
}
