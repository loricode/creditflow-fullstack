package co.crediflow.core;

import co.crediflow.shared.soap.RegistrarCreditoRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.xml.transform.StringSource;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.noFault;
import static org.springframework.ws.test.server.ResponseMatchers.validPayload;
import static org.springframework.ws.test.server.ResponseMatchers.xpath;

@SpringBootTest(properties = "core.tasa-fallo=0")
class CoreBancarioEndpointTest {

    private static final Map<String, String> NS = Map.of("cb", "http://crediflow.co/core-bancario");

    @Autowired
    private ApplicationContext context;

    @Test
    void registraCreditoYRespondeSegunElContratoXsd() throws Exception {
        StringSource peticion = new StringSource("""
                <cb:RegistrarCreditoRequest xmlns:cb="http://crediflow.co/core-bancario">
                  <cb:solicitudId>1</cb:solicitudId>
                  <cb:numeroDocumento>1032456789</cb:numeroDocumento>
                  <cb:nombreCompleto>Laura Gómez</cb:nombreCompleto>
                  <cb:producto>LIBRE_INVERSION</cb:producto>
                  <cb:monto>35000000</cb:monto>
                  <cb:plazoMeses>48</cb:plazoMeses>
                  <cb:tasaAnual>22.9</cb:tasaAnual>
                  <cb:cuotaMensual>1081234.55</cb:cuotaMensual>
                </cb:RegistrarCreditoRequest>
                """);

        MockWebServiceClient.createClient(context)
                .sendRequest(withPayload(peticion))
                .andExpect(noFault())
                .andExpect(validPayload(new ClassPathResource("core-bancario.xsd")))
                .andExpect(xpath("/cb:RegistrarCreditoResponse/cb:referencia", NS).exists())
                .andExpect(xpath("/cb:RegistrarCreditoResponse/cb:numeroCuenta", NS).exists());
    }

    @Test
    void esIdempotentePorSolicitud() {
        CoreBancarioEndpoint endpoint = new CoreBancarioEndpoint(0, () -> 1.0);
        RegistrarCreditoRequest request = new RegistrarCreditoRequest();
        request.setSolicitudId(7L);

        String primera = endpoint.registrar(request).getReferencia();
        String segunda = endpoint.registrar(request).getReferencia();

        assertThat(segunda).isEqualTo(primera);
    }

    @Test
    void lanzaFallaRecuperableCuandoElCoreNoEstaDisponible() {
        CoreBancarioEndpoint siempreFalla = new CoreBancarioEndpoint(1.0, () -> 0.0);

        assertThatThrownBy(() -> siempreFalla.registrar(new RegistrarCreditoRequest()))
                .isInstanceOf(CoreNoDisponibleException.class);
    }

    @Test
    void elDesembolsoCaeEnDiaHabil() {
        // 2026-11-20 es viernes -> lunes 23
        assertThat(CoreBancarioEndpoint.siguienteDiaHabil(LocalDate.of(2026, 11, 20))).isEqualTo(LocalDate.of(2026, 11, 23));
        assertThat(CoreBancarioEndpoint.siguienteDiaHabil(LocalDate.of(2026, 11, 18))).isEqualTo(LocalDate.of(2026, 11, 19));
    }
}
