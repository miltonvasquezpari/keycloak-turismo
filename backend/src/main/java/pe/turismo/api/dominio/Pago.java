package pe.turismo.api.dominio;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pago")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pago {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reserva_id", nullable = false, unique = true)
  private Reserva reserva;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal monto;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private MetodoPago metodo;

  @Column(name = "procesado_por", nullable = false, length = 60)
  private String procesadoPor;

  @Column(name = "procesado_en", nullable = false)
  private Instant procesadoEn;
}
