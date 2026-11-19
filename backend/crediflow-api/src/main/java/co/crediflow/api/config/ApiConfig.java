package co.crediflow.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiConfig implements WebMvcConfigurer {

    private final String[] origenesPermitidos;

    public ApiConfig(@Value("${crediflow.cors.origenes:http://localhost:4200}") String[] origenesPermitidos) {
        this.origenesPermitidos = origenesPermitidos;
    }

    @Bean
    OpenAPI crediflowOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CrediFlow API")
                .version("v1")
                .description("Originación de créditos: registro asíncrono, evaluación PL/SQL, simulador y estadísticas."));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origenesPermitidos)
                .allowedMethods("GET", "POST")
                .exposedHeaders("Location");
    }
}
