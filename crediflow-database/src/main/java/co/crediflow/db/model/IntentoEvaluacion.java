package co.crediflow.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "INTENTO_EVALUACION")
public class IntentoEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "SOLICITUD_ID", nullable = false)
    private Long solicitudId;

    @Column(name = "NUMERO_INTENTO", nullable = false)
    private Integer numeroIntento;

    @Enumerated(EnumType.STRING)
    @Column(name = "RESULTADO", nullable = false)
    private ResultadoIntento resultado;

    @Column(name = "ERROR", length = 1000)
    private String error;

    @Column(name = "DURACION_MS", nullable = false)
    private Long duracionMs;

    @Column(name = "CREADO_EN", nullable = false)
    private Instant creadoEn;

    protected IntentoEvaluacion() {
    }

    public static IntentoEvaluacion exito(Long solicitudId, int numero, long duracionMs) {
        return new IntentoEvaluacion(solicitudId, numero, ResultadoIntento.EXITO, null, duracionMs);
    }

    public static IntentoEvaluacion fallo(Long solicitudId, int numero, String error, long duracionMs) {
        String recortado = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
        return new IntentoEvaluacion(solicitudId, numero, ResultadoIntento.FALLO, recortado, duracionMs);
    }

    private IntentoEvaluacion(Long solicitudId, int numero, ResultadoIntento resultado, String error, long duracionMs) {
        this.solicitudId = solicitudId;
        this.numeroIntento = numero;
        this.resultado = resultado;
        this.error = error;
        this.duracionMs = duracionMs;
        this.creadoEn = Instant.now();
    }

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public Integer getNumeroIntento() { return numeroIntento; }
    public ResultadoIntento getResultado() { return resultado; }
    public String getError() { return error; }
    public Long getDuracionMs() { return duracionMs; }
    public Instant getCreadoEn() { return creadoEn; }
}
