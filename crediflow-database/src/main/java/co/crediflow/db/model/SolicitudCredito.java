package co.crediflow.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "SOLICITUD_CREDITO")
public class SolicitudCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "EXTERNAL_ID", nullable = false, updatable = false, length = 64)
    private String externalId;

    @Column(name = "NUMERO_DOCUMENTO", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(name = "NOMBRE_COMPLETO", nullable = false, length = 150)
    private String nombreCompleto;

    @Column(name = "EMAIL", nullable = false, length = 150)
    private String email;

    @Column(name = "INGRESOS_MENSUALES", nullable = false, precision = 15, scale = 2)
    private BigDecimal ingresosMensuales;

    @Column(name = "GASTOS_MENSUALES", nullable = false, precision = 15, scale = 2)
    private BigDecimal gastosMensuales;

    @Enumerated(EnumType.STRING)
    @Column(name = "TIPO_PRODUCTO", nullable = false, length = 20)
    private TipoProducto tipoProducto;

    @Column(name = "MONTO", nullable = false, precision = 15, scale = 2)
    private BigDecimal monto;

    @Column(name = "PLAZO_MESES", nullable = false)
    private Integer plazoMeses;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoSolicitud estado;

    @Column(name = "SCORE")
    private Integer score;

    @Enumerated(EnumType.STRING)
    @Column(name = "NIVEL_RIESGO", length = 10)
    private NivelRiesgo nivelRiesgo;

    @Column(name = "TASA_ANUAL", precision = 6, scale = 3)
    private BigDecimal tasaAnual;

    @Column(name = "CUOTA_MENSUAL", precision = 15, scale = 2)
    private BigDecimal cuotaMensual;

    @Lob
    @Column(name = "EXPLICACION_IA")
    private String explicacionIa;

    @Column(name = "PROVEEDOR_IA", length = 30)
    private String proveedorIa;

    @Column(name = "REFERENCIA_CORE", length = 40)
    private String referenciaCore;

    @Column(name = "S3_KEY", length = 300)
    private String s3Key;

    @Column(name = "ULTIMO_ERROR", length = 1000)
    private String ultimoError;

    @Column(name = "INTENTOS", nullable = false)
    private Integer intentos = 0;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Long version;

    @Column(name = "CREADO_EN", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "ACTUALIZADO_EN", nullable = false)
    private Instant actualizadoEn;

    @Column(name = "EVALUADO_EN")
    private Instant evaluadoEn;

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        if (creadoEn == null) {
            creadoEn = ahora;
        }
        actualizadoEn = ahora;
        if (estado == null) {
            estado = EstadoSolicitud.RECIBIDA;
        }
    }

    /** Transición a evaluación: cada entrada cuenta como un intento. */
    public void iniciarEvaluacion() {
        this.estado = EstadoSolicitud.EN_EVALUACION;
        this.intentos = (intentos == null ? 0 : intentos) + 1;
        this.ultimoError = null;
    }

    public void finalizar(EstadoSolicitud decision, String explicacion, String proveedor, String referencia) {
        if (!decision.esFinal()) {
            throw new IllegalArgumentException("Decisión no final: " + decision);
        }
        this.estado = decision;
        this.explicacionIa = explicacion;
        this.proveedorIa = proveedor;
        this.referenciaCore = referencia;
        this.evaluadoEn = Instant.now();
    }

    /** Fallo recuperable: vuelve a la cola a la espera del siguiente intento. */
    public void reintentoPendiente(String error) {
        this.estado = EstadoSolicitud.RECIBIDA;
        this.ultimoError = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
    }

    /**
     * ¿Puede un worker tomarla? Sí si está RECIBIDA, o si quedó EN_EVALUACION
     * más tiempo del razonable (el worker anterior murió a mitad de camino).
     */
    public boolean disponibleParaEvaluar(Instant limiteColgada) {
        return estado == EstadoSolicitud.RECIBIDA
                || (estado == EstadoSolicitud.EN_EVALUACION && actualizadoEn != null && actualizadoEn.isBefore(limiteColgada));
    }

    public void marcarError(String error) {
        this.estado = EstadoSolicitud.ERROR;
        this.ultimoError = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
    }

    /** Vuelve a la cola: solo desde ERROR. */
    public void reiniciar() {
        if (estado != EstadoSolicitud.ERROR) {
            throw new IllegalStateException("Solo se reintentan solicitudes en ERROR (estado actual: " + estado + ")");
        }
        this.estado = EstadoSolicitud.RECIBIDA;
        this.ultimoError = null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public BigDecimal getIngresosMensuales() { return ingresosMensuales; }
    public void setIngresosMensuales(BigDecimal ingresosMensuales) { this.ingresosMensuales = ingresosMensuales; }
    public BigDecimal getGastosMensuales() { return gastosMensuales; }
    public void setGastosMensuales(BigDecimal gastosMensuales) { this.gastosMensuales = gastosMensuales; }
    public TipoProducto getTipoProducto() { return tipoProducto; }
    public void setTipoProducto(TipoProducto tipoProducto) { this.tipoProducto = tipoProducto; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public Integer getPlazoMeses() { return plazoMeses; }
    public void setPlazoMeses(Integer plazoMeses) { this.plazoMeses = plazoMeses; }
    public EstadoSolicitud getEstado() { return estado; }
    public void setEstado(EstadoSolicitud estado) { this.estado = estado; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public NivelRiesgo getNivelRiesgo() { return nivelRiesgo; }
    public void setNivelRiesgo(NivelRiesgo nivelRiesgo) { this.nivelRiesgo = nivelRiesgo; }
    public BigDecimal getTasaAnual() { return tasaAnual; }
    public void setTasaAnual(BigDecimal tasaAnual) { this.tasaAnual = tasaAnual; }
    public BigDecimal getCuotaMensual() { return cuotaMensual; }
    public void setCuotaMensual(BigDecimal cuotaMensual) { this.cuotaMensual = cuotaMensual; }
    public String getExplicacionIa() { return explicacionIa; }
    public void setExplicacionIa(String explicacionIa) { this.explicacionIa = explicacionIa; }
    public String getProveedorIa() { return proveedorIa; }
    public void setProveedorIa(String proveedorIa) { this.proveedorIa = proveedorIa; }
    public String getReferenciaCore() { return referenciaCore; }
    public void setReferenciaCore(String referenciaCore) { this.referenciaCore = referenciaCore; }
    public String getS3Key() { return s3Key; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public String getUltimoError() { return ultimoError; }
    public void setUltimoError(String ultimoError) { this.ultimoError = ultimoError; }
    public Integer getIntentos() { return intentos; }
    public void setIntentos(Integer intentos) { this.intentos = intentos; }
    public Long getVersion() { return version; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public Instant getEvaluadoEn() { return evaluadoEn; }
    public void setEvaluadoEn(Instant evaluadoEn) { this.evaluadoEn = evaluadoEn; }
}
