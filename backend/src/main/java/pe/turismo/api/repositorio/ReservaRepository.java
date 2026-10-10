package pe.turismo.api.repositorio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.turismo.api.dominio.Reserva;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, String> {
  List<Reserva> findByClienteUsernameOrderByCreadaEnDesc(String clienteUsername);
  List<Reserva> findByAgenciaIdOrderByCreadaEnDesc(String agenciaId);

  /** Suma de personas en reservas no canceladas para un tour y fecha. */
  @Query("select coalesce(sum(r.personas), 0) from Reserva r " +
         "where r.tour.id = :tourId and r.fechaTour = :fecha " +
         "and r.estado <> pe.turismo.api.dominio.EstadoReserva.CANCELADA")
  int sumPersonasPorTourYFecha(@Param("tourId") Long tourId,
                               @Param("fecha")  LocalDate fecha);
}
