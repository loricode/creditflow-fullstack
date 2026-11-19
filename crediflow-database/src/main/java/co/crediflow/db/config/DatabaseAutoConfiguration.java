package co.crediflow.db.config;

import co.crediflow.db.plsql.CreditoPlsqlGateway;
import co.crediflow.db.plsql.EstadisticasPlsqlGateway;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Cualquier servicio que dependa de este módulo obtiene entidades, repositorios
 * y gateways PL/SQL sin configurar nada: el módulo se comporta como un starter.
 */
@AutoConfiguration(after = {JdbcTemplateAutoConfiguration.class}, before = HibernateJpaAutoConfiguration.class)
@EntityScan(basePackages = "co.crediflow.db.model")
@EnableJpaRepositories(basePackages = "co.crediflow.db.repository")
public class DatabaseAutoConfiguration {

    @Bean
    CreditoPlsqlGateway creditoPlsqlGateway(JdbcTemplate jdbcTemplate) {
        return new CreditoPlsqlGateway(jdbcTemplate);
    }

    @Bean
    EstadisticasPlsqlGateway estadisticasPlsqlGateway(JdbcTemplate jdbcTemplate) {
        return new EstadisticasPlsqlGateway(jdbcTemplate);
    }
}
