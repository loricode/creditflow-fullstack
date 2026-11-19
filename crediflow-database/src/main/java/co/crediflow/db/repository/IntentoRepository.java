package co.crediflow.db.repository;

import co.crediflow.db.model.IntentoEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntentoRepository extends JpaRepository<IntentoEvaluacion, Long> {

    List<IntentoEvaluacion> findBySolicitudIdOrderByNumeroIntentoAscIdAsc(Long solicitudId);
}
