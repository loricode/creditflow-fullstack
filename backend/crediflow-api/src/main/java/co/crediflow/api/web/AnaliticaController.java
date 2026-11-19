package co.crediflow.api.web;

import co.crediflow.api.mapper.SolicitudMapper;
import co.crediflow.api.service.AnaliticaService;
import co.crediflow.api.web.dto.EstadisticasResponse;
import co.crediflow.api.web.dto.SimulacionResponse;
import co.crediflow.db.model.TipoProducto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@Validated
@RequestMapping("/api/v1")
@Tag(name = "Analítica", description = "Simulador de cuota e indicadores, calculados por PL/SQL")
public class AnaliticaController {

    private final AnaliticaService analitica;
    private final SolicitudMapper mapper;

    public AnaliticaController(AnaliticaService analitica, SolicitudMapper mapper) {
        this.analitica = analitica;
        this.mapper = mapper;
    }

    @GetMapping("/simulador")
    @Operation(summary = "Simular cuota", description = "Usa PKG_CREDITO.CALCULAR_CUOTA con la tasa base del producto.")
    public SimulacionResponse simular(
            @RequestParam TipoProducto producto,
            @RequestParam @DecimalMin("1000000") @DecimalMax("2000000000") BigDecimal monto,
            @RequestParam @Min(6) @Max(360) int plazoMeses) {
        return mapper.aSimulacion(analitica.simular(producto, monto, plazoMeses));
    }

    @GetMapping("/estadisticas")
    @Operation(summary = "Indicadores del panel", description = "PKG_ESTADISTICAS.RESUMEN en una sola llamada.")
    public EstadisticasResponse estadisticas() {
        return mapper.aEstadisticas(analitica.resumen());
    }
}
