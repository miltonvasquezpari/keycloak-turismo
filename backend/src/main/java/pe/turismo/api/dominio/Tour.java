package pe.turismo.api.dominio;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "tour")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Tour {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 20, unique = true)
  private String codigo;

  @Column(nullable = false, length = 120)
  private String nombre;

  @Column(nullable = false, precision = 10, scale = 2)
  private BigDecimal precio;

  @Column(name = "duracion_horas", nullable = false)
  private int duracionHoras;

  @Column(name = "cupo_diario", nullable = false)
  private int cupoDiario;

  @Column(nullable = false)
  private boolean activo;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "destino_id", nullable = false)
  private Destino destino;
}
