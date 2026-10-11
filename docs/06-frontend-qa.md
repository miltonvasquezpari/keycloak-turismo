# Fase 6 — QA en navegador (cierre)

Resumen de pruebas end-to-end hechas con el frontend Angular corriendo
en `http://localhost:4200` contra el backend en `:8081` y Keycloak en
`:8080`.

## Fixes aplicados durante QA

| Commit | Qué arreglaba |
|---|---|
| `fix(fase-6): AppComponent sin crash al entrar sin sesion + rutas backend` | Entrada anónima al catálogo rompía por `KeycloakService` no inicializado todavía. |
| `fix(fase-6): catalogo enlaza por id numerico, no por codigo` | `/tours/:id` esperaba id; el catálogo pasaba `codigo`. |
| `fix(fase-6): reservar mapea DTO (tourId numerico, fechaTour, agenciaId)` | Form enviaba `tourCodigo`+`fecha`; DTO espera `tourId`+`fechaTour`+`agenciaId` (`LIM01` por defecto). |
| `fix(fase-6): mis-reservas sin volcado JSON; Ver abre alerta con detalle` | Ya no se mostraba JSON crudo bajo la lista. |
| `fix(fase-6): formulario de crear tour con campos del DTO (duracionHoras, cupoDiario, destinoId, activo)` | Faltaban 3 campos obligatorios del `CrearTourRequest`. |
| `fix(fase-6): finanzas-pagos alinea DTO (reservaId:string + metodo enum)` | El form mandaba `reservaId:number`+`monto`; DTO espera `reservaId:String`+`metodo` enum. |
| `fix(fase-6): CORS en cadena /api/reportes/** (preflight fallaba, impedia E12/E13)` | `reportesFilterChain` no tenía `.cors()`, el preflight del navegador se rompía. |

## Matriz de QA — flujos probados en navegador

| Flujo | Usuario | Resultado |
|---|---|---|
| E1 — ver catálogo público | sin sesión | OK |
| E2 — ver detalle de tour público | sin sesión | OK |
| E3 — listar mis reservas | ana.cliente | OK |
| E4 — crear reserva Machu Picchu | ana.cliente | OK, redirige a `/mis-reservas` |
| E5 — ver detalle de reserva propia | ana.cliente | OK (alerta con campos) |
| E6 — cancelar reserva PENDIENTE | ana.cliente | OK |
| E7 — reservas de LIM01 | lucia.agente | OK; no ve CUS01 |
| E8 — confirmar R-1001 | lucia.agente | OK → CONFIRMADA |
| E9 — crear tour T-NAZ-07 | olga.operador | OK |
| E10 — editar precio de un tour | olga.operador | OK |
| E11 — pagar R-1003 (CONFIRMADA) | fiona.finanzas con `pagos:write` | OK |
| E11 — 409 pagar R-1004 (PAGADA) | fiona.finanzas | OK (409 con mensaje "no está CONFIRMADA") |
| E11 — banner sin scope `pagos:write` | fiona.finanzas sin scope | Botón "Re-login con pagos:write" funciona |
| E12 — ver reporte de ventas | admin.turismo | OK (JSON con 4 reservas agregadas) |
| E13 — exportar CSV | admin.turismo | OK (descarga `ventas.csv`) |
| E13 — 403 granular en export | lucia.agente | OK ("Sin permiso para exportar") |

## Pendiente fuera de Fase 6

- E14 (partners multi-realm) no se prueba desde frontend; va con
  Postman en una fase posterior.
- Capturas en PNG dentro de `docs/evidencias/` quedan opcionales — las
  evidencias de granularidad de Fase 5 ya están en texto.
