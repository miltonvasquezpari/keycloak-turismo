package pe.turismo.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.turismo.api.dominio.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {}
