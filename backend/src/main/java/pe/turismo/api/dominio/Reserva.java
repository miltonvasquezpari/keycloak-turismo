package pe.turismo.api.dominio;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "reserva")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reserva {

  /** Formato textual R-NNNN. Lo asigna el servicio al crear. */
  @Id
  @Column(length = 20)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tour_id", nullable = false)
  private Tour tour;

  @Column(name = "fecha_tour", nullable = false)
  private LocalDate fechaTour;

  @Column(nullable = false)
  private int personas;

  @Column(name = "monto_total", nullable = false, precision = 10, scale = 2)
  private BigDecimal montoTotal;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoReserva estado;

  @Column(name = "cliente_username", nullable = false, length = 60)
  private String clienteUsername;

  @Column(name = "agencia_id", nullable = false, length = 20)
  private String agenciaId;

  @Column(name = "creada_en", nullable = false)
  private Instant creadaEn;
}
