package co.crediflow.shared.secrets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClientBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Carga credenciales desde AWS Secrets Manager antes de crear el DataSource,
 * para que ninguna contraseña viaje en variables de entorno ni en la imagen.
 *
 * <ul>
 *   <li>{@code crediflow.secrets.db-secret-id}: JSON {"username","password"} → spring.datasource.*</li>
 *   <li>{@code crediflow.secrets.ia-secret-id}: JSON {"api_key"} → crediflow.ia.api-key (si no está vacío)</li>
 * </ul>
 * Sin esas propiedades no hace nada (pruebas, perfil local sin AWS).
 */
public class SecretsManagerEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String FUENTE = "awsSecretsManager";
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment env, SpringApplication app) {
        String dbSecret = env.getProperty("crediflow.secrets.db-secret-id");
        String iaSecret = env.getProperty("crediflow.secrets.ia-secret-id");
        if (vacio(dbSecret) && vacio(iaSecret)) {
            return;
        }

        Map<String, Object> props = new HashMap<>();
        try (SecretsManagerClient client = crearCliente(env)) {
            if (!vacio(dbSecret)) {
                JsonNode db = leer(client, dbSecret);
                props.put("spring.datasource.username", db.path("username").asText());
                props.put("spring.datasource.password", db.path("password").asText());
            }
            if (!vacio(iaSecret)) {
                String apiKey = leer(client, iaSecret).path("api_key").asText("");
                if (!apiKey.isBlank()) {
                    props.put("crediflow.ia.api-key", apiKey);
                }
            }
        }
        // Precedencia máxima: el secreto manda sobre application.yml
        env.getPropertySources().addFirst(new MapPropertySource(FUENTE, props));
    }

    private JsonNode leer(SecretsManagerClient client, String secretId) {
        try {
            String valor = client.getSecretValue(r -> r.secretId(secretId)).secretString();
            return mapper.readTree(valor);
        } catch (Exception e) {
            // Fail-fast: sin credenciales el servicio no debe arrancar a medias
            throw new IllegalStateException("No se pudo leer el secreto '" + secretId + "' de Secrets Manager", e);
        }
    }

    private SecretsManagerClient crearCliente(ConfigurableEnvironment env) {
        SecretsManagerClientBuilder builder = SecretsManagerClient.builder()
                .region(Region.of(env.getProperty("crediflow.aws.region", "us-east-1")));
        String endpoint = env.getProperty("crediflow.aws.endpoint");
        if (!vacio(endpoint)) {
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    private static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
