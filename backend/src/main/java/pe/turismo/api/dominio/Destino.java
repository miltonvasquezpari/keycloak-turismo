package pe.turismo.api.dominio;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "destino")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Destino {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 80)
  private String nombre;

  @Column(nullable = false, length = 80)
  private String region;
}
