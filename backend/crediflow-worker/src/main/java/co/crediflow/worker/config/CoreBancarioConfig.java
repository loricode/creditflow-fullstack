package co.crediflow.worker.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

import java.time.Duration;

@Configuration
public class CoreBancarioConfig {

    @Bean
    Jaxb2Marshaller coreBancarioMarshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setPackagesToScan("co.crediflow.shared.soap");
        return marshaller;
    }

    @Bean
    WebServiceTemplate coreBancarioTemplate(Jaxb2Marshaller coreBancarioMarshaller,
                                            @Value("${crediflow.core-bancario.url}") String url,
                                            @Value("${crediflow.core-bancario.timeout-segundos:5}") int timeout) {
        HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();
        sender.setConnectionTimeout(Duration.ofSeconds(timeout));
        sender.setReadTimeout(Duration.ofSeconds(timeout));

        WebServiceTemplate template = new WebServiceTemplate(coreBancarioMarshaller);
        template.setDefaultUri(url);
        template.setMessageSender(sender);
        return template;
    }
}
