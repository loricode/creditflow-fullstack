package co.crediflow.db.plsql;

import co.crediflow.db.model.EstadoSolicitud;
import co.crediflow.db.model.NivelRiesgo;
import co.crediflow.db.model.TipoProducto;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.CallableStatement;
import java.sql.Types;

/**
 * Acceso al paquete PKG_CREDITO. Se usa JDBC directo (CallableStatement) en lugar
 * de SimpleJdbcCall para que el contrato con el procedimiento sea explícito y no
 * dependa de leer metadatos del diccionario de Oracle en cada arranque.
 */
public class CreditoPlsqlGateway {

    private static final String EVALUAR = "{call PKG_CREDITO.EVALUAR_SOLICITUD(?, ?, ?, ?, ?, ?)}";
    private static final String SIMULAR = """
            SELECT PKG_CREDITO.TASA_BASE(?) AS tasa,
                   PKG_CREDITO.CALCULAR_CUOTA(?, PKG_CREDITO.TASA_BASE(?), ?) AS cuota
              FROM dual
            """;

    private final JdbcTemplate jdbc;

    public CreditoPlsqlGateway(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Debe llamarse dentro de una transacción: el procedimiento no hace COMMIT. */
    public ResultadoEvaluacion evaluar(long solicitudId) {
        return jdbc.execute((java.sql.Connection con) -> con.prepareCall(EVALUAR), (CallableStatement cs) -> {
            cs.setLong(1, solicitudId);
            cs.registerOutParameter(2, Types.VARCHAR);
            cs.registerOutParameter(3, Types.NUMERIC);
            cs.registerOutParameter(4, Types.VARCHAR);
            cs.registerOutParameter(5, Types.NUMERIC);
            cs.registerOutParameter(6, Types.NUMERIC);
            cs.execute();
            return new ResultadoEvaluacion(
                    EstadoSolicitud.valueOf(cs.getString(2)),
                    cs.getInt(3),
                    NivelRiesgo.valueOf(cs.getString(4)),
                    cs.getBigDecimal(5),
                    cs.getBigDecimal(6));
        });
    }

    /** Simulación sin persistir: tasa base del producto y cuota estimada. */
    public Simulacion simular(TipoProducto producto, BigDecimal monto, int plazoMeses) {
        return jdbc.queryForObject(SIMULAR, (rs, i) -> {
            BigDecimal tasa = rs.getBigDecimal("tasa");
            BigDecimal cuota = rs.getBigDecimal("cuota").setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = cuota.multiply(BigDecimal.valueOf(plazoMeses));
            return new Simulacion(producto, monto, plazoMeses, tasa, cuota, total, total.subtract(monto));
        }, producto.name(), monto, producto.name(), plazoMeses);
    }
}
