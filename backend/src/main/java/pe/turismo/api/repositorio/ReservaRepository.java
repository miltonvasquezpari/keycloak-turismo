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

  /** Agregado por agencia y tour: cuenta, personas y monto. Excluye CANCELADAS. */
  @Query("""
    select r.agenciaId as agenciaId,
           r.tour.codigo as tourCodigo,
           r.tour.nombre as tourNombre,
           count(r)      as cantidadReservas,
           coalesce(sum(r.personas), 0) as personas,
           coalesce(sum(r.montoTotal), 0) as montoTotal
    from Reserva r
    where r.estado <> pe.turismo.api.dominio.EstadoReserva.CANCELADA
    group by r.agenciaId, r.tour.codigo, r.tour.nombre
    order by r.agenciaId, r.tour.codigo
  """)
  List<java.util.Map<String, Object>> agregarVentasPorAgenciaYTour();
}
