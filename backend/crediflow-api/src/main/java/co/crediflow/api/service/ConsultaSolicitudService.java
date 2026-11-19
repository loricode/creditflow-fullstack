package co.crediflow.api.service;

import co.crediflow.api.service.exception.RecursoNoEncontradoException;
import co.crediflow.db.model.CuotaAmortizacion;
import co.crediflow.db.model.EstadoSolicitud;
import co.crediflow.db.model.HistorialEstado;
import co.crediflow.db.model.IntentoEvaluacion;
import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.model.TipoProducto;
import co.crediflow.db.repository.AmortizacionRepository;
import co.crediflow.db.repository.HistorialRepository;
import co.crediflow.db.repository.IntentoRepository;
import co.crediflow.db.repository.SolicitudRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ConsultaSolicitudService {

    private final SolicitudRepository solicitudes;
    private final HistorialRepository historial;
    private final IntentoRepository intentos;
    private final AmortizacionRepository amortizacion;
    private final S3Client s3;
    private final String bucket;

    public ConsultaSolicitudService(SolicitudRepository solicitudes, HistorialRepository historial,
                                    IntentoRepository intentos, AmortizacionRepository amortizacion,
                                    S3Client s3, @Value("${crediflow.s3.bucket}") String bucket) {
        this.solicitudes = solicitudes;
        this.historial = historial;
        this.intentos = intentos;
        this.amortizacion = amortizacion;
        this.s3 = s3;
        this.bucket = bucket;
    }

    public Page<SolicitudCredito> buscar(EstadoSolicitud estado, TipoProducto producto, String texto, Pageable pageable) {
        Specification<SolicitudCredito> filtro = Specification.where(SolicitudSpecifications.conEstado(estado))
                .and(SolicitudSpecifications.conProducto(producto))
                .and(SolicitudSpecifications.contieneTexto(texto));
        return solicitudes.findAll(filtro, pageable);
    }

    public Detalle detalle(Long id) {
        SolicitudCredito solicitud = obtener(id);
        return new Detalle(solicitud,
                historial.findBySolicitudIdOrderByCambiadoEnAscIdAsc(id),
                intentos.findBySolicitudIdOrderByNumeroIntentoAscIdAsc(id));
    }

    public List<CuotaAmortizacion> amortizacion(Long id) {
        obtener(id);
        return amortizacion.findBySolicitudIdOrderByNumeroCuota(id);
    }

    /** JSON original archivado por el worker en S3. */
    public String original(Long id) {
        SolicitudCredito solicitud = obtener(id);
        if (solicitud.getS3Key() == null) {
            throw new RecursoNoEncontradoException("La solicitud " + id + " aún no tiene original archivado");
        }
        try {
            return s3.getObjectAsBytes(r -> r.bucket(bucket).key(solicitud.getS3Key())).asString(StandardCharsets.UTF_8);
        } catch (SdkException e) {
            throw new RecursoNoEncontradoException("No se pudo leer el original en S3: " + e.getMessage());
        }
    }

    private SolicitudCredito obtener(Long id) {
        return solicitudes.findById(id).orElseThrow(() -> RecursoNoEncontradoException.solicitud(id));
    }

    public record Detalle(SolicitudCredito solicitud, List<HistorialEstado> historial, List<IntentoEvaluacion> intentos) {
    }
}
