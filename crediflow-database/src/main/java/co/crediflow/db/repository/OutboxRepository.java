package co.crediflow.db.repository;

import co.crediflow.db.model.OutboxEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxRepository extends JpaRepository<OutboxEvento, Long> {

    /**
     * Toma un lote de eventos pendientes y los bloquea. SKIP LOCKED permite
     * varias réplicas del publicador sin que dos publiquen el mismo evento.
     * En Oracle, FOR UPDATE no admite FETCH FIRST, por eso se limita con ROWNUM.
     */
    @Query(value = """
            SELECT * FROM outbox_evento
             WHERE estado = 'PENDIENTE' AND ROWNUM <= :limite
               FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvento> bloquearPendientes(@Param("limite") int limite);
}
