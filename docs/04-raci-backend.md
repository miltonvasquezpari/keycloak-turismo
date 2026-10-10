# 04 · RACI del backend

Matriz RACI de los **14 endpoints** del backend contra los actores (grupos → roles → usuarios).

**Convenciones**
- **R** · ejecuta la operación.
- **A** · responsable (**una sola por fila**).
- **C** · consultado.
- **I** · informado.
- **–** · no participa.

Los partners (realm `agencias-partners`) se separan en dos columnas:
- **Partners/Nacionales** → `pedro.partner` (hoja `Arequipa-Tours`), `partner-agente`.
- **Partners-Admin** → `rosa.partner`, `partner-admin` (compuesto).

El service account `partners-integracion` (B2B máquina a máquina) comparte columna con pedro para E14.

## Matriz

| Endpoint                              | Clientes · `cliente`<br/>ana, carlos | Agencias/*/Ventas · `agente`<br/>lucia (LIM01), mario (CUS01) | Agencias/Lima/Supervision · `supervisor-agencia`<br/>jorge | Operaciones/Catalogo · `operador`<br/>olga | Operaciones/Finanzas · `pagos.procesar`+`reportes.ver`<br/>fiona | Administracion · `admin`<br/>admin.turismo | Partners/Nacionales · `partner-agente`<br/>pedro + integracion | Partners-Admin · `partner-admin`<br/>rosa |
|---|---|---|---|---|---|---|---|---|
| **E1** `GET /api/public/tours`              | I         | I            | I            | C   | –            | **A**   | I          | I          |
| **E2** `GET /api/public/tours/{id}`         | I         | I            | I            | C   | –            | **A**   | I          | I          |
| **E3** `GET /api/reservas/mias`             | R         | –            | –            | –   | –            | **A**   | –          | –          |
| **E4** `POST /api/reservas`                 | R         | I            | –            | –   | –            | **A**   | –          | –          |
| **E5** `GET /api/reservas/{id}`             | R (propia)| C (su agencia)| C (su agencia)| –  | I            | **A**   | –          | –          |
| **E6** `PUT /api/reservas/{id}/cancelar`    | R (propia, PENDIENTE) | I  | I            | –   | –            | **A**   | –          | –          |
| **E7** `GET /api/agencia/reservas`          | –         | R (su agencia)| **A**        | –   | –            | C       | –          | –          |
| **E8** `PUT /api/reservas/{id}/confirmar`   | I         | R            | **A** ¹      | –   | I            | C       | –          | –          |
| **E9** `POST /api/admin/tours`              | I         | I            | –            | R   | –            | **A**   | –          | –          |
| **E10** `PUT /api/admin/tours/{id}`         | I         | I            | –            | R   | –            | **A**   | –          | –          |
| **E11** `POST /api/pagos`                   | I         | I            | –            | –   | R            | **A**   | –          | –          |
| **E12** `GET /api/reportes/ventas`          | –         | –            | R            | –   | R            | **A**   | –          | –          |
| **E13** `GET /api/reportes/ventas/export`   | –         | –            | R (en horario)| –  | I            | **A**   | –          | –          |
| **E14** `GET /api/partners/disponibilidad`  | –         | –            | –            | C   | –            | I       | R          | **A**      |

¹ Jorge es supervisor: **ve pero no opera**. No tiene `reservas.confirmar` (su subgrupo `Supervision` no lo asigna). La responsabilidad de que la confirmación se haga queda en el supervisor, pero quien ejecuta es el agente.

## Lectura por fila (notas interpretativas)

- **E1, E2 (catálogo público):** cualquiera lee; Admin es responsable de que el catálogo esté publicado y correcto.
- **E3, E5, E6 (ciclo de vida de la propia reserva):** el cliente ejecuta sobre sus datos; Admin tiene la última palabra sobre reservas conflictivas.
- **E4 (crear reserva):** el cliente la crea; el agente de la agencia asignada se entera porque le llegará en su bandeja de E7.
- **E7, E8 (operación de la agencia):** el agente las ve y confirma; el supervisor es el responsable de la gestión diaria de la agencia.
- **E9, E10 (catálogo editable):** el operador ejecuta; Admin es responsable del catálogo.
- **E11 (pago):** Finanzas lo registra; Admin es responsable financiero (y requiere scope `pagos:write` como todos).
- **E12, E13 (reportes):** Supervisor y Finanzas los leen; Admin responsable. Export solo en horario laboral (política temporal, F5).
- **E14 (B2B):** Pedro (persona) o el service account `partners-integracion` (máquina) consultan disponibilidad; Rosa (`partner-admin`) es responsable del canal B2B.

## Resumen de permisos por actor

| Actor              | Rol realm        | Scopes default      | Puede escribir | Lee reportes | Alcance de datos |
|--------------------|------------------|---------------------|----------------|--------------|------------------|
| ana / carlos       | `cliente`        | `reservas:write` + `reportes:read` | sus reservas (E4) | – | solo propias |
| lucia / mario      | `agente`         | idem                | confirmar reservas (E8) de su agencia | – | agencia_id del token |
| jorge              | `supervisor-agencia` (hereda agente + `reportes.ver`) | idem | ve pero no confirma | sí (E12, E13 en horario) | LIM01 |
| olga               | `operador`       | idem                | catálogo (E9, E10) | – | – |
| fiona              | — (sin realm role) · roles cliente: `pagos.procesar`+`reportes.ver` | idem; además `pagos:write` opcional | pagos (E11) | sí (E12) | – |
| admin              | `admin` (todo)   | idem; `pagos:write` opcional | todo | sí | todo |
| pedro + integracion| `partner-agente` + `disponibilidad.consultar` (realm B2B) | `disponibilidad:read` | – (solo lectura) | – | – |
| rosa               | `partner-admin` (hereda `partner-agente`) + `disponibilidad.consultar` | idem | – (solo lectura) | – | – |

Esta tabla es el contrato que Fase 4 debe implementar en el backend: cada regla aquí se refleja en una línea de `application.yml` (`turismo.security.rules`) o en una anotación `@PreAuthorize` / `@PostAuthorize`.
