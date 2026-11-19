package co.crediflow.worker.evaluacion;

import co.crediflow.db.model.EstadoSolicitud;
import co.crediflow.db.model.NivelRiesgo;
import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.model.TipoProducto;
import co.crediflow.db.plsql.ResultadoEvaluacion;
import co.crediflow.worker.config.WorkerProperties;
import co.crediflow.worker.ia.Explicacion;
import co.crediflow.worker.ia.ExplicacionIaService;
import co.crediflow.worker.integracion.ArchivoOriginalService;
import co.crediflow.worker.integracion.CoreBancarioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EvaluacionServiceTest {

    private final SolicitudEstadoService estado = mock(SolicitudEstadoService.class);
    private final ArchivoOriginalService archivo = mock(ArchivoOriginalService.class);
    private final ExplicacionIaService ia = mock(ExplicacionIaService.class);
    private final CoreBancarioClient core = mock(CoreBancarioClient.class);
    private EvaluacionService servicio;
    private SolicitudCredito solicitud;

    @BeforeEach
    void setUp() {
        WorkerProperties props = new WorkerProperties("http://cola", 1, 3, 10, "bucket");
        servicio = new EvaluacionService(estado, archivo, ia, core, props);

        solicitud = new SolicitudCredito();
        solicitud.setId(10L);
        solicitud.setTipoProducto(TipoProducto.VEHICULO);
        solicitud.setMonto(new BigDecimal("40000000"));
        solicitud.setPlazoMeses(60);
        solicitud.setIngresosMensuales(new BigDecimal("9000000"));
        solicitud.setGastosMensuales(new BigDecimal("3000000"));
        solicitud.setCreadoEn(Instant.now());

        when(archivo.archivar(any())).thenReturn("solicitudes/x.json");
        when(ia.explicar(any(), any())).thenReturn(new Explicacion("texto", "reglas"));
    }

    private static ResultadoEvaluacion resultado(EstadoSolicitud decision) {
        return new ResultadoEvaluacion(decision, 812, NivelRiesgo.BAJO, new BigDecimal("16.9"), new BigDecimal("950000"));
    }

    @Test
    void aprobadaSeRegistraEnElCoreYFinaliza() {
        ResultadoEvaluacion aprobada = resultado(EstadoSolicitud.APROBADA);
        when(estado.iniciar(10L)).thenReturn(Optional.of(solicitud));
        when(estado.evaluar(10L, "solicitudes/x.json")).thenReturn(aprobada);
        when(core.registrar(solicitud, aprobada)).thenReturn("CB-2026-1001");

        servicio.procesar(10L, 1);

        verify(estado).finalizar(eq(10L), eq(aprobada), any(Explicacion.class), eq("CB-2026-1001"), anyLong());
    }

    @Test
    void rechazadaNoLlamaAlCore() {
        ResultadoEvaluacion rechazada = resultado(EstadoSolicitud.RECHAZADA);
        when(estado.iniciar(10L)).thenReturn(Optional.of(solicitud));
        when(estado.evaluar(10L, "solicitudes/x.json")).thenReturn(rechazada);

        servicio.procesar(10L, 1);

        verifyNoInteractions(core);
        verify(estado).finalizar(eq(10L), eq(rechazada), any(Explicacion.class), eq(null), anyLong());
    }

    @Test
    void mensajeDuplicadoSeDescartaSinTrabajo() {
        when(estado.iniciar(10L)).thenReturn(Optional.empty());

        servicio.procesar(10L, 1);

        verifyNoInteractions(archivo, ia, core);
    }

    @Test
    void falloRecuperableSeRegistraYPideReintento() {
        ResultadoEvaluacion aprobada = resultado(EstadoSolicitud.APROBADA);
        when(estado.iniciar(10L)).thenReturn(Optional.of(solicitud));
        when(estado.evaluar(10L, "solicitudes/x.json")).thenReturn(aprobada);
        when(core.registrar(solicitud, aprobada)).thenThrow(new IllegalStateException("SOAP Fault"));

        assertThatThrownBy(() -> servicio.procesar(10L, 1)).isInstanceOf(EvaluacionFallidaException.class);

        verify(estado).registrarFallo(eq(10L), anyString(), anyLong(), eq(false));
        verify(estado, never()).finalizar(any(), any(), any(), any(), anyLong());
    }

    @Test
    void enElUltimoIntentoQuedaEnError() {
        when(estado.iniciar(10L)).thenReturn(Optional.of(solicitud));
        when(archivo.archivar(any())).thenThrow(new IllegalStateException("S3 caído"));

        assertThatThrownBy(() -> servicio.procesar(10L, 3)).isInstanceOf(EvaluacionFallidaException.class);

        verify(estado).registrarFallo(eq(10L), anyString(), anyLong(), eq(true));
    }

    @Test
    void errorNoRecuperableNoReintenta() {
        when(estado.iniciar(10L)).thenReturn(Optional.of(solicitud));
        when(estado.evaluar(10L, "solicitudes/x.json")).thenThrow(new NoRecuperableException("ORA-20002", null));

        servicio.procesar(10L, 1); // no lanza: el mensaje se borra

        verify(estado).registrarFallo(eq(10L), eq("ORA-20002"), anyLong(), eq(true));
    }
}
