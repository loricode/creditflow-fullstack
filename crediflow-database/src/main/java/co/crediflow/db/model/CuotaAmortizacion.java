package co.crediflow.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila de la tabla de amortización. La genera PKG_CREDITO; en Java es de solo lectura. */
@Entity
@Immutable
@Table(name = "AMORTIZACION")
public class CuotaAmortizacion {

    @Id
    private Long id;

    @Column(name = "SOLICITUD_ID")
    private Long solicitudId;

    @Column(name = "NUMERO_CUOTA")
    private Integer numeroCuota;

    @Column(name = "FECHA_PAGO")
    private LocalDate fechaPago;

    @Column(name = "CUOTA")
    private BigDecimal cuota;

    @Column(name = "CAPITAL")
    private BigDecimal capital;

    @Column(name = "INTERES")
    private BigDecimal interes;

    @Column(name = "SALDO")
    private BigDecimal saldo;

    protected CuotaAmortizacion() {
    }

    public CuotaAmortizacion(Long id, Long solicitudId, Integer numeroCuota, LocalDate fechaPago,
                             BigDecimal cuota, BigDecimal capital, BigDecimal interes, BigDecimal saldo) {
        this.id = id;
        this.solicitudId = solicitudId;
        this.numeroCuota = numeroCuota;
        this.fechaPago = fechaPago;
        this.cuota = cuota;
        this.capital = capital;
        this.interes = interes;
        this.saldo = saldo;
    }

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public Integer getNumeroCuota() { return numeroCuota; }
    public LocalDate getFechaPago() { return fechaPago; }
    public BigDecimal getCuota() { return cuota; }
    public BigDecimal getCapital() { return capital; }
    public BigDecimal getInteres() { return interes; }
    public BigDecimal getSaldo() { return saldo; }
}
