package co.crediflow.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/** Auditoría de cambios de estado. La escribe el trigger TRG_SOLICITUD_HISTORIAL. */
@Entity
@Immutable
@Table(name = "SOLICITUD_HISTORIAL")
public class HistorialEstado {

    @Id
    private Long id;

    @Column(name = "SOLICITUD_ID")
    private Long solicitudId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_ANTERIOR")
    private EstadoSolicitud estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO_NUEVO")
    private EstadoSolicitud estadoNuevo;

    @Column(name = "CAMBIADO_EN")
    private Instant cambiadoEn;

    protected HistorialEstado() {
    }

    public Long getId() { return id; }
    public Long getSolicitudId() { return solicitudId; }
    public EstadoSolicitud getEstadoAnterior() { return estadoAnterior; }
    public EstadoSolicitud getEstadoNuevo() { return estadoNuevo; }
    public Instant getCambiadoEn() { return cambiadoEn; }
}
