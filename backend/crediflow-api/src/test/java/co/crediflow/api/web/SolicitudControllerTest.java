package co.crediflow.api.web;

import co.crediflow.api.mapper.SolicitudMapperImpl;
import co.crediflow.api.service.ConsultaSolicitudService;
import co.crediflow.api.service.SolicitudService;
import co.crediflow.api.service.exception.RecursoNoEncontradoException;
import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.model.TipoProducto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitudController.class)
@Import(SolicitudMapperImpl.class)
class SolicitudControllerTest {

    private static final String VALIDA = """
            {"externalId":"k-1","numeroDocumento":"1032456789","nombreCompleto":"Laura Gómez",
             "email":"laura@correo.co","ingresosMensuales":8500000,"gastosMensuales":3200000,
             "tipoProducto":"VEHICULO","monto":35000000,"plazoMeses":48}
            """;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private SolicitudService solicitudService;

    @MockBean
    private ConsultaSolicitudService consultaService;

    private static SolicitudCredito guardada() {
        SolicitudCredito s = new SolicitudCredito();
        s.setId(5L);
        s.setExternalId("k-1");
        s.setNombreCompleto("Laura Gómez");
        s.setTipoProducto(TipoProducto.VEHICULO);
        s.setMonto(new BigDecimal("35000000"));
        s.setCreadoEn(Instant.now());
        return s;
    }

    @Test
    void registrarDevuelve201ConLocation() throws Exception {
        when(solicitudService.registrar(any())).thenReturn(new SolicitudService.ResultadoRegistro(guardada(), true));

        mvc.perform(post("/api/v1/solicitudes").contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/solicitudes/5"))
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void registrarRepetidaDevuelve200() throws Exception {
        when(solicitudService.registrar(any())).thenReturn(new SolicitudService.ResultadoRegistro(guardada(), false));

        mvc.perform(post("/api/v1/solicitudes").contentType(MediaType.APPLICATION_JSON).content(VALIDA))
                .andExpect(status().isOk());
    }

    @Test
    void datosInvalidosDevuelvenProblemDetailConCampos() throws Exception {
        String invalida = """
                {"numeroDocumento":"12","nombreCompleto":"","email":"no-es-correo",
                 "ingresosMensuales":100,"gastosMensuales":0,"tipoProducto":"VEHICULO","monto":500,"plazoMeses":2}
                """;

        mvc.perform(post("/api/v1/solicitudes").contentType(MediaType.APPLICATION_JSON).content(invalida))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", "application/problem+json"))
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores.numeroDocumento").exists())
                .andExpect(jsonPath("$.errores.email").exists())
                .andExpect(jsonPath("$.errores.monto").exists())
                .andExpect(jsonPath("$.errores.plazoMeses").exists());
    }

    @Test
    void detalleInexistenteDevuelve404() throws Exception {
        when(consultaService.detalle(99L)).thenThrow(RecursoNoEncontradoException.solicitud(99L));

        mvc.perform(get("/api/v1/solicitudes/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe la solicitud 99"));
    }

    @Test
    void estadoDesconocidoEnElFiltroDevuelve400() throws Exception {
        mvc.perform(get("/api/v1/solicitudes").param("estado", "INVENTADO"))
                .andExpect(status().isBadRequest());
    }
}
