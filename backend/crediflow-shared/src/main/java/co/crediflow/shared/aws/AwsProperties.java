package co.crediflow.shared.aws;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * @param region   región de AWS
 * @param endpoint endpoint alternativo (LocalStack en local). Vacío en AWS real.
 */
@ConfigurationProperties(prefix = "crediflow.aws")
public record AwsProperties(String region, URI endpoint) {

    public AwsProperties {
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }
    }

    public boolean tieneEndpoint() {
        return endpoint != null && !endpoint.toString().isBlank();
    }
}
