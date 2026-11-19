package co.crediflow.shared.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;

@XmlRootElement(name = "RegistrarCreditoRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"solicitudId", "numeroDocumento", "nombreCompleto", "producto",
        "monto", "plazoMeses", "tasaAnual", "cuotaMensual"})
public class RegistrarCreditoRequest {

    @XmlElement(required = true)
    private long solicitudId;
    @XmlElement(required = true)
    private String numeroDocumento;
    @XmlElement(required = true)
    private String nombreCompleto;
    @XmlElement(required = true)
    private String producto;
    @XmlElement(required = true)
    private BigDecimal monto;
    @XmlElement(required = true)
    private int plazoMeses;
    @XmlElement(required = true)
    private BigDecimal tasaAnual;
    @XmlElement(required = true)
    private BigDecimal cuotaMensual;

    public long getSolicitudId() { return solicitudId; }
    public void setSolicitudId(long solicitudId) { this.solicitudId = solicitudId; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getProducto() { return producto; }
    public void setProducto(String producto) { this.producto = producto; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public int getPlazoMeses() { return plazoMeses; }
    public void setPlazoMeses(int plazoMeses) { this.plazoMeses = plazoMeses; }
    public BigDecimal getTasaAnual() { return tasaAnual; }
    public void setTasaAnual(BigDecimal tasaAnual) { this.tasaAnual = tasaAnual; }
    public BigDecimal getCuotaMensual() { return cuotaMensual; }
    public void setCuotaMensual(BigDecimal cuotaMensual) { this.cuotaMensual = cuotaMensual; }
}
