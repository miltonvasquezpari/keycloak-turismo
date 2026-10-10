package pe.turismo.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.turismo.api.dominio.Destino;

public interface DestinoRepository extends JpaRepository<Destino, Long> {}
