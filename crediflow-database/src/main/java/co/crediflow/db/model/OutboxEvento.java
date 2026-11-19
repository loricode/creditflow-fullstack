package co.crediflow.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Patrón outbox transaccional: el evento se guarda en la misma transacción que
 * la solicitud y un proceso aparte lo publica en SNS. Así nunca hay solicitud
 * sin evento ni evento sin solicitud.
 */
@Entity
@Table(name = "OUTBOX_EVENTO")
public class OutboxEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "AGREGADO_ID", nullable = false)
    private Long agregadoId;

    @Column(name = "TIPO", nullable = false, length = 60)
    private String tipo;

    @Lob
    @Column(name = "PAYLOAD", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 15)
    private EstadoOutbox estado = EstadoOutbox.PENDIENTE;

    @Column(name = "INTENTOS", nullable = false)
    private Integer intentos = 0;

    @Column(name = "ULTIMO_ERROR", length = 1000)
    private String ultimoError;

    @Column(name = "CREADO_EN", nullable = false, updatable = false)
    private Instant creadoEn;

    @Column(name = "PUBLICADO_EN")
    private Instant publicadoEn;

    protected OutboxEvento() {
    }

    public OutboxEvento(Long agregadoId, String tipo, String payload) {
        this.agregadoId = agregadoId;
        this.tipo = tipo;
        this.payload = payload;
        this.creadoEn = Instant.now();
    }

    public void marcarPublicado() {
        this.estado = EstadoOutbox.PUBLICADO;
        this.publicadoEn = Instant.now();
        this.ultimoError = null;
    }

    public void registrarFallo(String error, int maxIntentos) {
        this.intentos = intentos + 1;
        this.ultimoError = error == null ? null : error.substring(0, Math.min(error.length(), 1000));
        if (intentos >= maxIntentos) {
            this.estado = EstadoOutbox.FALLIDO;
        }
    }

    public Long getId() { return id; }
    public Long getAgregadoId() { return agregadoId; }
    public String getTipo() { return tipo; }
    public String getPayload() { return payload; }
    public EstadoOutbox getEstado() { return estado; }
    public Integer getIntentos() { return intentos; }
    public String getUltimoError() { return ultimoError; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getPublicadoEn() { return publicadoEn; }
}
