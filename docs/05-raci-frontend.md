# 05 · RACI del frontend

Matriz RACI de las **9 pantallas** de `turismo-web` contra los actores
(grupos → roles → usuarios). Mismas columnas que el Anexo A / `04-raci-backend.md`.

**Convenciones**
- **R** · ejecuta la operación.
- **A** · responsable (**una sola por fila**).
- **C** · consultado.
- **I** · informado.
- **–** · no participa / no tiene acceso.

Los partners no tienen frontend aquí: `turismo-web` sirve solo al realm `turismo`.

## Matriz

| Pantalla / Ruta                        | Guards                        | Endpoints | Clientes · `cliente`<br/>ana, carlos | Agencias/*/Ventas · `agente`<br/>lucia, mario | Agencias/Lima/Supervision · `supervisor-agencia`<br/>jorge | Operaciones/Catalogo · `operador`<br/>olga | Operaciones/Finanzas · `pagos.procesar`+`reportes.ver`<br/>fiona | Administración · `admin`<br/>admin.turismo |
|---|---|---|---|---|---|---|---|---|
| `/` **Catálogo**                       | público                       | E1        | R          | R   | R   | R   | R   | **A** |
| `/tours/:id` **Detalle**               | público                       | E2        | R          | R   | R   | R   | R   | **A** |
| `/reservar/:tourId` **Reservar**       | `cliente`                     | E4        | R / **A**  | –   | –   | –   | –   | I   |
| `/mis-reservas` **Mis reservas**       | `cliente`                     | E3,E5,E6  | R / **A**  | –   | –   | –   | –   | I   |
| `/agencia` **Agencia**                 | `agente` + claim `agencia_id` | E7,E8     | –          | R / **A** | I   | –   | –   | I   |
| `/operaciones/tours` **Operaciones**   | `tours.gestionar`             | E9,E10    | –          | –   | –   | R / **A** | –   | I   |
| `/finanzas/pagos` **Finanzas**         | `pagos.procesar` + scope `pagos:write` | E11       | –          | –   | –   | –   | R / **A** | I   |
| `/reportes` **Reportes**               | autenticado                   | E12,E13   | –          | –   | R   | –   | R   | **A** |
| `/acceso-denegado` **403 amable**      | público                       | —         | I          | I   | I   | I   | I   | **A** |

## Visibilidad por rol en el menú (header)

| Entrada del menú | Visible si… |
|---|---|
| Catálogo (`/`) | siempre |
| Mis reservas | `cliente` |
| Agencia (muestra `agencia_id`) | `agente` |
| Operaciones | `tours.gestionar` |
| Finanzas | `pagos.procesar` |
| Reportes | `supervisor-agencia` ∨ `admin` ∨ `reportes.ver` |
| Ingresar / Salir | según `KeycloakService.isLoggedIn()` |

La directiva estructural `*tieneRol="'…'"` oculta los enlaces si el usuario no
cumple el requisito, y el guard `hasRole(...)` evita el acceso directo por URL.

## Notas de alcance (lo que la rúbrica fija)

- `lucia` (grupo `Agencias/Agencia-Lima/Ventas`) ve solo reservas con
  `agencia_id = LIM01`; `mario` solo `CUS01`. El backend filtra por el claim
  del token (ver §4.3 del plan).
- Para `/finanzas/pagos`, el token inicial no trae `pagos:write`; la pantalla
  detecta la ausencia del scope y ofrece **re-login** con
  `scope=openid pagos:write`.
- Para `/reportes`, la visibilidad del botón de exportar **no** se calcula
  consultando UMA en cliente; se muestra siempre (fallback indicado en el
  plan) y se maneja la respuesta 403.
