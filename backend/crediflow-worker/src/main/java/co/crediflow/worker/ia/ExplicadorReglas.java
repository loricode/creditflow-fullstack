package co.crediflow.worker.ia;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Explicación determinista basada en las mismas reglas de PKG_CREDITO.
 * Es el respaldo cuando la IA no está configurada, falla o se niega a responder:
 * el solicitante siempre recibe una explicación.
 */
@Component
public class ExplicadorReglas {

    static final String PROVEEDOR = "reglas";
    private static final BigDecimal LIMITE_CARGA = BigDecimal.valueOf(40);

    public Explicacion explicar(DatosDecision d) {
        StringBuilder sb = new StringBuilder();
        if ("APROBADA".equals(d.decision())) {
            sb.append("Tu solicitud fue aprobada con un puntaje de ").append(d.score())
              .append(" (riesgo ").append(d.nivelRiesgo().toLowerCase(Locale.ROOT)).append("). ")
              .append("La cuota mensual estimada es de ").append(pesos(d.cuotaMensual()))
              .append(" a una tasa de ").append(pct(d.tasaAnual())).append(" % E.A., lo que representa el ")
              .append(pct(d.cargaCuota())).append(" % de tu capacidad de pago. ")
              .append("El desembolso se programa en el siguiente día hábil.");
        } else {
            sb.append("Por ahora no fue posible aprobar tu solicitud (puntaje ").append(d.score()).append("). ");
            if (d.capacidadPago().signum() <= 0) {
                sb.append("Tus gastos mensuales igualan o superan tus ingresos. ");
            } else if (d.cargaCuota().compareTo(LIMITE_CARGA) > 0) {
                sb.append("La cuota de ").append(pesos(d.cuotaMensual())).append(" ocuparía el ")
                  .append(pct(d.cargaCuota())).append(" % de tu capacidad de pago y el máximo permitido es 40 %. ");
            } else {
                sb.append("Tu nivel de endeudamiento actual (").append(pct(d.endeudamiento())).append(" %) es alto. ");
            }
            sb.append("Puedes intentarlo con un monto menor o un plazo más largo, o reduciendo tus obligaciones actuales.");
        }
        return new Explicacion(sb.toString(), PROVEEDOR);
    }

    private static String pct(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-CO"));
        formato.setMaximumFractionDigits(1);
        return formato.format(valor);
    }

    private static String pesos(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));
        formato.setMaximumFractionDigits(0);
        return formato.format(valor);
    }
}
