package pe.turismo.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.turismo.api.dominio.Reserva;

import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, String> {
  List<Reserva> findByClienteUsernameOrderByCreadaEnDesc(String clienteUsername);
  List<Reserva> findByAgenciaIdOrderByCreadaEnDesc(String agenciaId);
}
