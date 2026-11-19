package co.crediflow.db.repository;

import co.crediflow.db.model.SolicitudCredito;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SolicitudRepository extends JpaRepository<SolicitudCredito, Long>, JpaSpecificationExecutor<SolicitudCredito> {

    Optional<SolicitudCredito> findByExternalId(String externalId);

    /** SELECT ... FOR UPDATE: solo un worker puede tomar la solicitud a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SolicitudCredito s where s.id = :id")
    Optional<SolicitudCredito> bloquear(@Param("id") Long id);
}
