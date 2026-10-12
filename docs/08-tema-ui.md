# Fase 8 — Tema personalizado `turismo-peru`

Tema de **login** propio aplicado a los realms `turismo` y
`agencias-partners`. Reemplaza la pantalla por defecto de Keycloak con
una identidad TurismoPerú (paleta andina + logo chakana) usando solo
CSS: cero imágenes externas, cero licencias que gestionar.

## Estructura

```
keycloak/themes/turismo-peru/login/
├── theme.properties
├── resources/
│   ├── css/turismo.css
│   └── img/logo.svg
└── messages/
    └── messages_es.properties
```

## `theme.properties`

```
parent=keycloak
import=common/keycloak
styles=css/login.css css/turismo.css
locales=es,en
```

Hereda del tema `keycloak` clásico (freemarker). Se evaluó `keycloak.v2`
(nuevo diseño PatternFly v5 por defecto en 26.x), pero el clásico da
más control con selectores sencillos y basta para los puntos de la
rúbrica. Se documenta por si se decide migrar más adelante.

## Paleta y fondo

| Token | Valor | Uso |
|---|---|---|
| `--terracota` | `#C0502A` | Botón primario, aro del logo |
| `--andino` | `#2F6B4F` | Títulos, enlaces, chakana |
| `--arena` | `#F4EDE1` | Fondo de la tarjeta |
| `--tinta` | `#1b1f23` | Texto |

Fondo = **gradiente diagonal ocre → terracota → verde andino** +
**patrón geométrico chakana** (SVG embebido como `data:` URI, opacidad
0.08) que tapiza suavemente la página. Sin fotos → repo pequeño, sin
atribuciones legales.

## Logo

`resources/img/logo.svg` — chakana (cruz escalonada andina) en verde
sobre disco arena con aro terracota. SVG puro, 1 KB.

## Textos custom (`messages_es.properties`)

- `loginTitle` → "Bienvenido a TurismoPerú"
- `doLogIn` → "Ingresar"
- `doRegister` → "Crear cuenta"
- `doForgotPassword` → "¿Olvidó su contraseña?"
- `noAccount` → "¿Nuevo en TurismoPerú?"
- `identity-provider-login-label` → "O ingrese con"
- `invalidPasswordRegexPatternMessage` → "La contraseña debe incluir
  al menos un carácter especial: @ % & # . + / ! = ? ¿"

## Asignación

Vía `kcadm.sh` en el contenedor (persistido también en los JSON de
realm para re-imports):

```
kcadm.sh update realms/turismo           -s loginTheme=turismo-peru
kcadm.sh update realms/agencias-partners -s loginTheme=turismo-peru
```

Keycloak corre en `start-dev`, que **desactiva el caché de temas** —
los cambios en CSS/HTML se reflejan en el próximo refresh del
navegador sin reiniciar el contenedor.

## Prueba manual

1. En `http://localhost:4200` → **Iniciar sesión**.
2. Debe verse: fondo degradado con patrón chakana, tarjeta arena
   centrada, logo redondo arriba, botón **Ingresar** terracota.
3. Clic en **Crear cuenta** → la pantalla de registro usa el mismo
   tema y los textos en español.
4. Clic en **¿Olvidó su contraseña?** → mismo tema.
5. (Si la realm tiene la *password policy* `regexPattern` configurada
   exigiendo un carácter especial) → al poner una contraseña sin
   especial, se ve el mensaje personalizado en español.
6. Pantalla de Keycloak del realm `agencias-partners`
   (`http://localhost:8080/realms/agencias-partners/account`) también
   luce con el mismo tema.

## Evidencias

Capturas en `docs/evidencias/`:

- `fase8-login.png` — pantalla de login con tema aplicado.
- `fase8-registro.png` — pantalla de "Crear cuenta".
- `fase8-forgot.png` — pantalla "¿Olvidó su contraseña?".
- `fase8-password-regla.png` — error de contraseña con mensaje
  personalizado (si aplica la policy).
