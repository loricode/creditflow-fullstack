package co.crediflow.db.plsql;

import co.crediflow.db.model.TipoProducto;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Acceso a PKG_ESTADISTICAS.RESUMEN: escalares por OUT y dos SYS_REFCURSOR. */
public class EstadisticasPlsqlGateway {

    private static final String RESUMEN = "{call PKG_ESTADISTICAS.RESUMEN(?, ?, ?, ?, ?, ?, ?, ?, ?)}";

    private final JdbcTemplate jdbc;

    public EstadisticasPlsqlGateway(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public ResumenEstadisticas resumen() {
        return jdbc.execute((java.sql.Connection con) -> con.prepareCall(RESUMEN), (CallableStatement cs) -> {
            for (int i = 1; i <= 7; i++) {
                cs.registerOutParameter(i, Types.NUMERIC);
            }
            cs.registerOutParameter(8, Types.REF_CURSOR);
            cs.registerOutParameter(9, Types.REF_CURSOR);
            cs.execute();

            List<ResumenEstadisticas.PorProducto> porProducto = new ArrayList<>();
            try (ResultSet rs = cs.getObject(8, ResultSet.class)) {
                while (rs.next()) {
                    porProducto.add(new ResumenEstadisticas.PorProducto(
                            TipoProducto.valueOf(rs.getString("tipo_producto")),
                            rs.getLong("total"),
                            rs.getLong("aprobadas"),
                            rs.getBigDecimal("monto_aprobado")));
                }
            }

            List<ResumenEstadisticas.PorDia> ultimosDias = new ArrayList<>();
            try (ResultSet rs = cs.getObject(9, ResultSet.class)) {
                while (rs.next()) {
                    ultimosDias.add(new ResumenEstadisticas.PorDia(
                            rs.getDate("dia").toLocalDate(),
                            rs.getLong("total"),
                            rs.getLong("aprobadas")));
                }
            }

            return new ResumenEstadisticas(
                    cs.getLong(1), cs.getLong(2), cs.getLong(3), cs.getLong(4), cs.getLong(5),
                    cs.getBigDecimal(6), cs.getInt(7), porProducto, ultimosDias);
        });
    }
}
