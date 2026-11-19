package co.crediflow.db.repository;

import co.crediflow.db.model.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialRepository extends JpaRepository<HistorialEstado, Long> {

    List<HistorialEstado> findBySolicitudIdOrderByCambiadoEnAscIdAsc(Long solicitudId);
}
