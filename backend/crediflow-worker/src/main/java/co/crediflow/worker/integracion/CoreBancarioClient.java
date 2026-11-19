package co.crediflow.worker.integracion;

import co.crediflow.db.model.SolicitudCredito;
import co.crediflow.db.plsql.ResultadoEvaluacion;
import co.crediflow.shared.soap.RegistrarCreditoRequest;
import co.crediflow.shared.soap.RegistrarCreditoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

/** Cliente SOAP del core bancario. Los SOAP Fault y timeouts se propagan: el worker reintenta. */
@Component
public class CoreBancarioClient {

    private static final Logger log = LoggerFactory.getLogger(CoreBancarioClient.class);

    private final WebServiceTemplate template;

    public CoreBancarioClient(WebServiceTemplate coreBancarioTemplate) {
        this.template = coreBancarioTemplate;
    }

    public String registrar(SolicitudCredito solicitud, ResultadoEvaluacion resultado) {
        RegistrarCreditoRequest request = new RegistrarCreditoRequest();
        request.setSolicitudId(solicitud.getId());
        request.setNumeroDocumento(solicitud.getNumeroDocumento());
        request.setNombreCompleto(solicitud.getNombreCompleto());
        request.setProducto(solicitud.getTipoProducto().name());
        request.setMonto(solicitud.getMonto());
        request.setPlazoMeses(solicitud.getPlazoMeses());
        request.setTasaAnual(resultado.tasaAnual());
        request.setCuotaMensual(resultado.cuotaMensual());

        RegistrarCreditoResponse response = (RegistrarCreditoResponse) template.marshalSendAndReceive(request);
        log.info("Core bancario registró la solicitud {} con referencia {}", solicitud.getId(), response.getReferencia());
        return response.getReferencia();
    }
}
