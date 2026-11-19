package co.crediflow.worker.integracion;

import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.worker.config.WorkerProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Archiva en S3 la solicitud tal como llegó. Particiones estilo Hive
 * (anio=/mes=/producto=) para consultarlas después con Athena sin mover datos.
 * La clave es determinista: un reintento sobrescribe el mismo objeto.
 */
@Component
public class ArchivoOriginalService {

    private final S3Client s3;
    private final ObjectMapper mapper;
    private final String bucket;

    public ArchivoOriginalService(S3Client s3, ObjectMapper mapper, WorkerProperties props) {
        this.s3 = s3;
        this.mapper = mapper;
        this.bucket = props.bucket();
    }

    public String archivar(SolicitudCredito s) {
        ZonedDateTime fecha = s.getCreadoEn().atZone(ZoneOffset.UTC);
        String key = "solicitudes/anio=%d/mes=%02d/producto=%s/solicitud-%d.json"
                .formatted(fecha.getYear(), fecha.getMonthValue(), s.getTipoProducto(), s.getId());

        s3.putObject(r -> r.bucket(bucket).key(key)
                        .contentType("application/json")
                        .serverSideEncryption(ServerSideEncryption.AES256),
                RequestBody.fromString(json(s)));
        return key;
    }

    private String json(SolicitudCredito s) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", s.getId());
        doc.put("externalId", s.getExternalId());
        doc.put("numeroDocumento", s.getNumeroDocumento());
        doc.put("nombreCompleto", s.getNombreCompleto());
        doc.put("email", s.getEmail());
        doc.put("ingresosMensuales", s.getIngresosMensuales());
        doc.put("gastosMensuales", s.getGastosMensuales());
        doc.put("tipoProducto", s.getTipoProducto());
        doc.put("monto", s.getMonto());
        doc.put("plazoMeses", s.getPlazoMeses());
        doc.put("creadoEn", s.getCreadoEn().toString());
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar la solicitud " + s.getId(), e);
        }
    }
}
