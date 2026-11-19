package co.crediflow.worker.evaluacion;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.UncategorizedSQLException;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitudEstadoServiceTest {

    @Test
    void reconoceErroresDeNegocioDeRaiseApplicationError() {
        SQLException ora20002 = new SQLException("ORA-20002: estado inválido", "72000", 20002);
        assertThat(SolicitudEstadoService.esErrorDeNegocioPlsql(new UncategorizedSQLException("call", "sql", ora20002))).isTrue();
    }

    @Test
    void erroresDeInfraestructuraSonRecuperables() {
        SQLException conexion = new SQLException("ORA-12541: no listener", "08006", 12541);
        assertThat(SolicitudEstadoService.esErrorDeNegocioPlsql(new DataAccessResourceFailureException("caída", conexion))).isFalse();
    }
}
