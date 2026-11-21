# Suite E2E de Atlas: Cucumber + Selenium + OWASP ZAP (EP3)

Esta suite recorre el **frontend de Atlas** (`/app/...`) con un navegador real, como lo haría
una persona usuaria, y al mismo tiempo hace pasar todo el tráfico por **OWASP ZAP**. Sigue la
misma estructura que el lab S14 del curso (`bdd-tests` sobre Juice Shop).

```
Feature en Gherkin (español) → Step definitions (Java) → Selenium (Chromium) → Atlas
                                                              │
                                                              └─ proxy OWASP ZAP (pasivo + activo)
```

## Estructura

| Ruta | Contenido |
| --- | --- |
| `src/test/resources/features/*.feature` | 4 características, 16 escenarios (contando cada fila de los Ejemplos), en español (`# language: es`) |
| `src/test/java/.../steps/` | `SesionSteps`, `ClienteSteps`, `SeguridadSteps` |
| `src/test/java/.../support/Ui.java` | Selectores y acciones centralizados; esperas explícitas (sin `Thread.sleep`) |
| `src/test/java/.../support/Navegador.java` | Chromium remoto (Docker) o Chrome local; proxy ZAP opcional |
| `src/test/java/.../hooks/Hooks.java` | `@Before` navegador limpio · `@After` captura `<uc>_<caso>_<escenario>_<OK\|FALLA>.png` |
| `src/test/java/.../hooks/ZapHook.java` | `@AfterAll`: escaneo pasivo, spider y **escaneo activo autenticado**; reportes HTML/JSON |
| `docker-compose.yml` | Servicios `atlas-api`, `zap`, `selenium` y `tests` |

## Etiquetas

Cada escenario lleva la etiqueta de su característica (`@uc01` a `@uc04`), su tipo
(`@funcional` o `@seguridad`) y el **ID del caso de prueba del informe** (`@CP-SEG-05`, etc.), que
mantiene la trazabilidad con la EP1 y la EP2.

| Característica | Escenarios | Casos |
| --- | --- | --- |
| `uc01` Inicio y cierre de sesión | 4 | CP-FUN-03, CP-SEG-02, CP-SEG-03, CP-FUN-04 |
| `uc02` Gestión de clientes | 5 | CP-FUN-06, CP-FUN-08 (×2), CP-FUN-10, CP-FUN-01 |
| `uc03` Contratos con estado | 1 | CP-FUN-02, CP-INT-02 |
| `uc04` Seguridad desde el navegador | 6 | CP-SEG-05 (XSS), CP-SEG-07 (SQLi ×2), CP-SEG-01, CP-SEG-04, CP-SEG-08 |

## Ejecutar

### Opción A: Docker Compose (recomendada, igual que el lab)

Requiere solo Docker Desktop. Desde **esta carpeta** (`App/bdd-e2e`):

```bash
docker compose up --build --abort-on-container-exit --exit-code-from tests
```

La primera vez tarda (descarga imágenes y dependencias Maven). El escaneo activo de ZAP agrega
hasta 10 minutos al final. Para ver el navegador en vivo: <http://localhost:7900> (clave `secret`).

Variantes:

```bash
TAGS=@seguridad docker compose up --build --abort-on-container-exit --exit-code-from tests
ZAP_ESCANEO_ACTIVO=false docker compose up --build --abort-on-container-exit --exit-code-from tests
docker compose down      # al terminar
```

En Windows PowerShell las variables se definen antes: `$env:TAGS="@seguridad"`.

### Opción B: local, sin Docker para las pruebas

Con Atlas corriendo en `http://localhost:8080` (por ejemplo `cd ../atlas-api && ./mvnw spring-boot:run`)
y Google Chrome instalado:

```bash
mvn test                                        # todos los escenarios, sin ZAP
mvn test -Dheadless=false                       # viendo el navegador
mvn test -Dcucumber.filter.tags="@uc04"         # solo una característica
mvn test -Dcucumber.execution.dry-run=true      # verifica que cada paso tenga su step
```

> El bloqueo por fuerza bruta usa un usuario inexistente distinto en cada corrida, y los clientes
> creados llevan un sufijo aleatorio: la suite se puede repetir sin reiniciar la base.

## Resultados y evidencias

| Archivo | Uso en el informe |
| --- | --- |
| `target/cucumber-report/cucumber.html` | Captura del resumen (escenarios aprobados) |
| `target/cucumber-report/cucumber.json` / `.xml` | Integración con CI |
| `target/evidencias/capturas/*.png` | Una captura por escenario |
| `target/evidencias/zap/zap-report.html` | Alertas de ZAP por riesgo |
| `target/evidencias/zap/zap-alerts.json` | Detalle de alertas (para la tabla de hallazgos) |

Copiar estas evidencias a `evidencias/EP3/08-e2e-selenium/` (reporte y capturas) y
`evidencias/EP3/09-zap-e2e/` (reportes de ZAP) en la raíz del repositorio.

## Problemas frecuentes

| Síntoma | Causa probable | Solución |
| --- | --- | --- |
| `tests` espera indefinidamente | Un servicio no pasa el healthcheck | `docker compose ps` y `docker compose logs zap` |
| `TimeoutException` en un paso | Cambió un id del HTML | Ajustar el selector en `Ui.java` |
| CP-SEG-03 falla con "Demasiados intentos" desde el primer intento | Ejecución repetida con el mismo usuario | La suite ya usa un usuario nuevo; revisar que `Contexto.usuarioNuevo` se use |
| `Undefined step` | Texto del feature distinto al del step | `mvn test -Dcucumber.execution.dry-run=true` |
| ZAP sin alertas del API | El navegador no pasó por el proxy | Revisar `ZAP_URL` y el log `[ZAP] Proxy activo` |
