package pe.turismo.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.turismo.api.dominio.Tour;

public interface TourRepository extends JpaRepository<Tour, Long> {}
