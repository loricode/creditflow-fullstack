package co.crediflow.core;

import co.crediflow.shared.soap.CoreBancarioNs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/** WSDL publicado en /ws/core-bancario.wsdl, generado a partir del XSD. */
@Configuration
public class WebServiceConfig {

    @Bean
    XsdSchema coreBancarioSchema() {
        return new SimpleXsdSchema(new ClassPathResource("core-bancario.xsd"));
    }

    @Bean(name = "core-bancario")
    DefaultWsdl11Definition coreBancarioWsdl(XsdSchema coreBancarioSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("CoreBancarioPort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace(CoreBancarioNs.URI);
        wsdl.setSchema(coreBancarioSchema);
        return wsdl;
    }
}
