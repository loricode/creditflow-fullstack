package co.crediflow.core;

import co.crediflow.shared.soap.CoreBancarioNs;
import co.crediflow.shared.soap.RegistrarCreditoRequest;
import co.crediflow.shared.soap.RegistrarCreditoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.DoubleSupplier;

/**
 * Simula el core bancario heredado. Dos comportamientos de un sistema real:
 * <ul>
 *   <li>Idempotente por solicitudId: un reintento devuelve la misma referencia.</li>
 *   <li>Fallas intermitentes (probabilidad configurable) para ejercitar los reintentos del worker.</li>
 * </ul>
 */
@Endpoint
public class CoreBancarioEndpoint {

    private static final Logger log = LoggerFactory.getLogger(CoreBancarioEndpoint.class);

    private final Map<Long, RegistrarCreditoResponse> registrados = new ConcurrentHashMap<>();
    private final AtomicLong consecutivo = new AtomicLong(1000);
    private final double tasaFallo;
    private final DoubleSupplier azar;

    @Autowired
    public CoreBancarioEndpoint(@Value("${core.tasa-fallo:0.2}") double tasaFallo) {
        this(tasaFallo, () -> ThreadLocalRandom.current().nextDouble());
    }

    CoreBancarioEndpoint(double tasaFallo, DoubleSupplier azar) {
        this.tasaFallo = tasaFallo;
        this.azar = azar;
    }

    @PayloadRoot(namespace = CoreBancarioNs.URI, localPart = "RegistrarCreditoRequest")
    @ResponsePayload
    public RegistrarCreditoResponse registrar(@RequestPayload RegistrarCreditoRequest request) {
        RegistrarCreditoResponse previo = registrados.get(request.getSolicitudId());
        if (previo != null) {
            log.info("Solicitud {} ya registrada: {}", request.getSolicitudId(), previo.getReferencia());
            return previo;
        }
        if (azar.getAsDouble() < tasaFallo) {
            log.warn("Falla simulada registrando la solicitud {}", request.getSolicitudId());
            throw new CoreNoDisponibleException("Core bancario temporalmente no disponible (simulado)");
        }

        RegistrarCreditoResponse respuesta = new RegistrarCreditoResponse();
        respuesta.setReferencia("CB-" + Year.now().getValue() + "-" + consecutivo.incrementAndGet());
        respuesta.setNumeroCuenta("CR" + String.format("%010d", ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L)));
        respuesta.setFechaDesembolso(siguienteDiaHabil(LocalDate.now()).toString());
        registrados.put(request.getSolicitudId(), respuesta);

        log.info("Crédito registrado solicitud={} producto={} monto={} ref={}",
                request.getSolicitudId(), request.getProducto(), request.getMonto(), respuesta.getReferencia());
        return respuesta;
    }

    static LocalDate siguienteDiaHabil(LocalDate fecha) {
        LocalDate siguiente = fecha.plusDays(1);
        while (siguiente.getDayOfWeek() == DayOfWeek.SATURDAY || siguiente.getDayOfWeek() == DayOfWeek.SUNDAY) {
            siguiente = siguiente.plusDays(1);
        }
        return siguiente;
    }
}
