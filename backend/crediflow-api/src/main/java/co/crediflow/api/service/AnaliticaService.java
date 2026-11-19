package co.crediflow.api.service;

import co.crediflow.db.model.TipoProducto;
import co.crediflow.db.plsql.CreditoPlsqlGateway;
import co.crediflow.db.plsql.EstadisticasPlsqlGateway;
import co.crediflow.db.plsql.ResumenEstadisticas;
import co.crediflow.db.plsql.Simulacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/** Lecturas resueltas por paquetes PL/SQL: simulador e indicadores del panel. */
@Service
@Transactional(readOnly = true)
public class AnaliticaService {

    private final CreditoPlsqlGateway credito;
    private final EstadisticasPlsqlGateway estadisticas;

    public AnaliticaService(CreditoPlsqlGateway credito, EstadisticasPlsqlGateway estadisticas) {
        this.credito = credito;
        this.estadisticas = estadisticas;
    }

    public Simulacion simular(TipoProducto producto, BigDecimal monto, int plazoMeses) {
        return credito.simular(producto, monto, plazoMeses);
    }

    public ResumenEstadisticas resumen() {
        return estadisticas.resumen();
    }
}
