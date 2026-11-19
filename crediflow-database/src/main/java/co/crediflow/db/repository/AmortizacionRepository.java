package co.crediflow.db.repository;

import co.crediflow.db.model.CuotaAmortizacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AmortizacionRepository extends JpaRepository<CuotaAmortizacion, Long> {

    List<CuotaAmortizacion> findBySolicitudIdOrderByNumeroCuota(Long solicitudId);
}
