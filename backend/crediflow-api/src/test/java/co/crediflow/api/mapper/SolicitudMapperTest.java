package co.crediflow.api.mapper;

import co.crediflow.api.web.dto.CrearSolicitudRequest;
import co.crediflow.api.web.dto.EstadisticasResponse;
import co.crediflow.db.model.EstadoSolicitud;
import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.model.TipoProducto;
import co.crediflow.db.plsql.ResumenEstadisticas;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitudMapperTest {

    private final SolicitudMapper mapper = new SolicitudMapperImpl();

    @Test
    void laEntidadNuevaNoTomaEstadoNiResultadoDelCliente() {
        CrearSolicitudRequest req = new CrearSolicitudRequest("k", "1032456789", "Laura", "l@c.co",
                new BigDecimal("8000000"), new BigDecimal("2000000"), TipoProducto.VIVIENDA,
                new BigDecimal("200000000"), 240);

        SolicitudCredito s = mapper.aEntidad(req);

        assertThat(s.getTipoProducto()).isEqualTo(TipoProducto.VIVIENDA);
        assertThat(s.getPlazoMeses()).isEqualTo(240);
        assertThat(s.getEstado()).isNull(); // lo fija @PrePersist
        assertThat(s.getScore()).isNull();
    }

    @Test
    void tasaDeAprobacionSobreDecididas() {
        ResumenEstadisticas r = new ResumenEstadisticas(10, 3, 1, 5, 1, BigDecimal.TEN, 700, List.of(), List.of());

        EstadisticasResponse e = mapper.aEstadisticas(r);

        assertThat(e.tasaAprobacion()).isEqualTo(75.0);
        assertThat(e.enProceso()).isEqualTo(5);
    }

    @Test
    void sinDecididasLaTasaEsCero() {
        ResumenEstadisticas r = new ResumenEstadisticas(2, 0, 0, 2, 0, BigDecimal.ZERO, 0, List.of(), List.of());
        assertThat(mapper.aEstadisticas(r).tasaAprobacion()).isZero();
    }

    @Test
    void elResumenConservaElEstado() {
        SolicitudCredito s = new SolicitudCredito();
        s.setEstado(EstadoSolicitud.EN_EVALUACION);
        assertThat(mapper.aResumen(s).estado()).isEqualTo(EstadoSolicitud.EN_EVALUACION);
    }
}
