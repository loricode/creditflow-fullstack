package co.crediflow.db.plsql;

import co.crediflow.db.model.EstadoSolicitud;
import co.crediflow.db.model.NivelRiesgo;
import co.crediflow.db.model.TipoProducto;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.oracle.OracleContainer;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Prueba los paquetes PL/SQL, triggers y migraciones contra un Oracle real
 * (gvenzl/oracle-free). Requiere Docker. Se ejecuta con {@code mvn verify}.
 */
@Testcontainers
class PaquetesPlsqlIT {

    @Container
    static final OracleContainer ORACLE = new OracleContainer("gvenzl/oracle-free:23-slim-faststart")
            .withUsername("crediflow")
            .withPassword("crediflow")
            .withStartupTimeout(Duration.ofMinutes(4));

    static JdbcTemplate jdbc;
    static CreditoPlsqlGateway credito;
    static EstadisticasPlsqlGateway estadisticas;

    @BeforeAll
    static void migrar() {
        Flyway.configure()
                .dataSource(ORACLE.getJdbcUrl(), ORACLE.getUsername(), ORACLE.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(ORACLE.getJdbcUrl(), ORACLE.getUsername(), ORACLE.getPassword()));
        credito = new CreditoPlsqlGateway(jdbc);
        estadisticas = new EstadisticasPlsqlGateway(jdbc);
    }

    private static long crear(String producto, long ingresos, long gastos, long monto, int plazo) {
        GeneratedKeyHolder key = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO solicitud_credito (external_id, numero_documento, nombre_completo, email,
                        ingresos_mensuales, gastos_mensuales, tipo_producto, monto, plazo_meses)
                    VALUES (SYS_GUID(), '1032456789', 'Prueba', 'p@c.co', ?, ?, ?, ?, ?)
                    """, new String[]{"ID"});
            ps.setLong(1, ingresos);
            ps.setLong(2, gastos);
            ps.setString(3, producto);
            ps.setLong(4, monto);
            ps.setInt(5, plazo);
            return ps;
        }, key);
        long id = key.getKey().longValue();
        jdbc.update("UPDATE solicitud_credito SET estado = 'EN_EVALUACION' WHERE id = ?", id);
        return id;
    }

    /** Flyway no falla si un paquete o trigger compila con errores: esta prueba sí. */
    @Test
    void noQuedanObjetosPlsqlInvalidos() {
        List<String> invalidos = jdbc.queryForList(
                "SELECT object_type || ' ' || object_name FROM user_objects WHERE status = 'INVALID'", String.class);
        assertThat(invalidos).isEmpty();
    }

    @Test
    void perfilSolventeSeApruebaYGeneraAmortizacionCompleta() {
        long id = crear("LIBRE_INVERSION", 8_000_000, 3_000_000, 30_000_000, 48);

        ResultadoEvaluacion r = credito.evaluar(id);

        assertThat(r.decision()).isEqualTo(EstadoSolicitud.APROBADA);
        assertThat(r.score()).isEqualTo(850);
        assertThat(r.nivelRiesgo()).isEqualTo(NivelRiesgo.BAJO);
        assertThat(r.tasaAnual()).isEqualByComparingTo("22.9");

        List<BigDecimal> saldos = jdbc.queryForList(
                "SELECT saldo FROM amortizacion WHERE solicitud_id = ? ORDER BY numero_cuota", BigDecimal.class, id);
        assertThat(saldos).hasSize(48);
        assertThat(saldos.get(47)).isEqualByComparingTo("0");

        BigDecimal capital = jdbc.queryForObject("SELECT SUM(capital) FROM amortizacion WHERE solicitud_id = ?", BigDecimal.class, id);
        assertThat(capital).isEqualByComparingTo("30000000");
    }

    @Test
    void sobreendeudadoSeRechazaSinAmortizacion() {
        long id = crear("LIBRE_INVERSION", 3_000_000, 2_500_000, 50_000_000, 36);

        ResultadoEvaluacion r = credito.evaluar(id);

        assertThat(r.decision()).isEqualTo(EstadoSolicitud.RECHAZADA);
        assertThat(r.nivelRiesgo()).isEqualTo(NivelRiesgo.ALTO);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM amortizacion WHERE solicitud_id = ?", Integer.class, id)).isZero();
    }

    @Test
    void reevaluarEsIdempotente() {
        long id = crear("VEHICULO", 9_000_000, 2_000_000, 40_000_000, 60);

        credito.evaluar(id);
        credito.evaluar(id);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM amortizacion WHERE solicitud_id = ?", Integer.class, id)).isEqualTo(60);
    }

    @Test
    void elTriggerAuditaCadaCambioDeEstado() {
        long id = crear("EDUCACION", 6_000_000, 1_000_000, 15_000_000, 24);

        List<String> estados = jdbc.queryForList(
                "SELECT estado_nuevo FROM solicitud_historial WHERE solicitud_id = ? ORDER BY id", String.class, id);

        assertThat(estados).containsExactly("RECIBIDA", "EN_EVALUACION");
    }

    @Test
    void noEvaluaSolicitudesFueraDeEvaluacion() {
        long id = crear("VIVIENDA", 12_000_000, 3_000_000, 200_000_000, 240);
        jdbc.update("UPDATE solicitud_credito SET estado = 'RECIBIDA' WHERE id = ?", id);

        assertThatThrownBy(() -> credito.evaluar(id))
                .isInstanceOf(UncategorizedSQLException.class)
                .hasMessageContaining("ORA-20002");
    }

    @Test
    void simuladorCalculaCuotaFrancesa() {
        Simulacion s = credito.simular(TipoProducto.VIVIENDA, new BigDecimal("100000000"), 180);

        assertThat(s.tasaAnual()).isEqualByComparingTo("12.5");
        // 12,5 % EA -> 0,9863 % mensual -> cuota ≈ 1.192.000
        assertThat(s.cuotaMensual()).isBetween(new BigDecimal("1180000"), new BigDecimal("1205000"));
        assertThat(s.totalIntereses()).isPositive();
    }

    @Test
    void estadisticasDevuelvenSerieDeSieteDias() {
        ResumenEstadisticas r = estadisticas.resumen();

        assertThat(r.total()).isPositive();
        assertThat(r.ultimosDias()).hasSize(7);
        assertThat(r.porProducto()).isNotEmpty();
    }
}
