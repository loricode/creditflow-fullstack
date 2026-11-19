package co.crediflow.shared.aws;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Clientes AWS con la misma configuración en todos los servicios. Con
 * {@code crediflow.aws.endpoint} apuntan a LocalStack; sin él, a AWS real con
 * la cadena de credenciales por defecto (rol IAM de la tarea/instancia).
 */
@AutoConfiguration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsClientsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    S3Client s3Client(AwsProperties props) {
        return configurar(S3Client.builder(), props)
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(props.tieneEndpoint()).build())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    SqsClient sqsClient(AwsProperties props) {
        return configurar(SqsClient.builder(), props).build();
    }

    @Bean
    @ConditionalOnMissingBean
    SnsClient snsClient(AwsProperties props) {
        return configurar(SnsClient.builder(), props).build();
    }

    static <B extends AwsClientBuilder<B, C>, C> B configurar(B builder, AwsProperties props) {
        builder.region(Region.of(props.region()));
        if (props.tieneEndpoint()) {
            builder.endpointOverride(props.endpoint());
        }
        return builder;
    }
}
