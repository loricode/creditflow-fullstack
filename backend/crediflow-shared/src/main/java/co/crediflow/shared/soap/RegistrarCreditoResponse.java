package co.crediflow.shared.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "RegistrarCreditoResponse")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"referencia", "numeroCuenta", "fechaDesembolso"})
public class RegistrarCreditoResponse {

    @XmlElement(required = true)
    private String referencia;
    @XmlElement(required = true)
    private String numeroCuenta;
    @XmlElement(required = true)
    private String fechaDesembolso;

    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String numeroCuenta) { this.numeroCuenta = numeroCuenta; }
    public String getFechaDesembolso() { return fechaDesembolso; }
    public void setFechaDesembolso(String fechaDesembolso) { this.fechaDesembolso = fechaDesembolso; }
}
