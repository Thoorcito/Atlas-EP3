# Atlas · Evaluación Parcial 3 (ISY1102)

**Optimizando el aseguramiento de la calidad y seguridad del software**

Tercera entrega del caso semestral **Atlas** (CreaLab SpA, Caso A): plataforma de gestión de
clientes y contratos para pymes chilenas. Sobre el MVP de la EP2 (API Spring Boot con 14
herramientas de calidad y seguridad), esta entrega agrega lo necesario para **probar el sistema
como lo usa una persona**, **activar los análisis que estaban bloqueados** y **asegurar que la
calidad se mantenga** en las próximas iteraciones.

| Integrante | Responsabilidad en la EP3 |
| --- | --- |
| **Felipe Bravo Aracena** | Código, pipeline y preparación de toda la entrega: frontend, suite E2E, SonarCloud y Snyk en el pipeline, diagramas, plan de mantenimiento y borrador del informe |
| **Pablo Astroza Navarro** | Validar el entorno y ejecutar las pruebas: compilación, suite E2E con ZAP, Lighthouse, pipeline, regresión y evidencias (sube `evidencias/EP3/`) |
| **Yanfranco Cariqueo Donoso** | Informe final: completar resultados, incidentes nuevos, análisis, conclusiones y veredicto (sube el informe a `informe/`) |

## 1. Estructura del repositorio

```
.
├── README.md                      ← este archivo
├── .github/workflows/
│   └── calidad-codigo.yml         ← pipeline de 14 jobs (SonarCloud y Snyk activos en la EP3)
├── App/
│   ├── atlas-api/                 ← aplicación (antes atlas-ep2/): API + frontend /app/
│   └── bdd-e2e/                   ← suite Cucumber + Selenium + OWASP ZAP
├── informe/
│   ├── Informe_EP2_Atlas.docx     ← informe de la EP2 (referencia)
│   └── diagramas/                 ← diagramas de la EP3 en PNG y su fuente HTML
└── evidencias/
    ├── EP2/                       ← evidencias de la EP2
    └── EP3/                       ← evidencias de la EP3 (07-frontend … 13-regresion)
```

## 2. Cambios en el código respecto de la EP2

### 2.1 Frontend mínimo (`App/atlas-api/src/main/resources/static/app/`)

| Página | Función | Requerimiento |
| --- | --- | --- |
| `/app/index.html` | Inicio de sesión con mensajes de bloqueo y sesión expirada | RF-1.1 |
| `/app/clientes.html` | Listado, búsqueda y paginación; registro y eliminación con confirmación (solo ADMIN) | RF-4.1–4.3, RB-4 |
| `/app/contratos.html` | Contratos del cliente con estado ACTIVO / POR VENCER / EXPIRADO | RF-5.1, RF-5.4 |

Decisiones de seguridad y usabilidad:

- **Sin scripts ni estilos en línea**: cumple la CSP existente (`script-src 'self'`).
- Los datos se escriben **solo con `textContent`**, nunca con `innerHTML` (defensa contra XSS).
- El JWT vive en `sessionStorage` (no en cookies, así que no hay CSRF); ante un 401 se borra y se
  vuelve al login. El riesgo residual está documentado como INC-14.
- El rol EDITOR no ve acciones de escritura, y el backend igual responde 403 si las fuerza.
- Pensado para adultos mayores (ERS): texto base de 18 px, contraste AA, botones de 48 px, foco
  visible, etiquetas en todos los campos y mensajes con `role="alert"`.

Cambios en Java (mínimos): `SecurityConfig` permite `/app/**` (solo HTML, CSS y JS; los datos
siguen exigiendo JWT) y `SeguridadIntegracionTest` agrega una prueba que verifica que el frontend
es público y lleva la CSP.

### 2.2 Suite E2E (`App/bdd-e2e/`)

Cucumber + Selenium + OWASP ZAP, con la misma estructura del lab S14 del curso. Detalle, etiquetas
y ejecución en [`App/bdd-e2e/README.md`](App/bdd-e2e/README.md).

```bash
cd App/bdd-e2e
docker compose up --build --abort-on-container-exit --exit-code-from tests
```

### 2.3 Pipeline: SonarCloud y Snyk activos (`.github/workflows/calidad-codigo.yml`)

Solo se modificaron los **jobs 8 y 9**; no se agregaron jobs nuevos.

| Job | EP2 | EP3 |
| --- | --- | --- |
| 8 · SonarCloud | Si faltaba el token terminaba **en verde** con «pendiente» | **Falla** si falta `SONAR_TOKEN`, `SONAR_ORGANIZATION` o `SONAR_PROJECT_KEY`; analiza Java **y el JavaScript del frontend** con la cobertura de JaCoCo; **espera el Quality Gate** y falla si no aprueba; publica métricas en el resumen |
| 9 · Snyk | Igual: verde sin analizar | **Falla** si falta `SNYK_TOKEN`; genera `snyk-report.json`; **puerta**: CRITICAL no aceptada = falla, HIGH = advertencia; informa cuántas vulnerabilidades están aceptadas; `snyk monitor` en `main` para alertas de CVE nuevas |

También se actualizó `App/atlas-api/.snyk`: los riesgos aceptados ahora **vencen el 31-03-2027**,
tienen responsable y el motivo correcto (la rama Spring Boot 3.5 ya no recibe parches públicos).

#### Secretos del pipeline (configurar una vez)

GitHub → *Settings → Secrets and variables → Actions*:

| Nombre | Pestaña | Dónde se obtiene |
| --- | --- | --- |
| `SONAR_TOKEN` | Secrets | sonarcloud.io → My Account → Security |
| `SONAR_ORGANIZATION` | Variables | Organization Key en SonarCloud |
| `SONAR_PROJECT_KEY` | Variables | Project Key en SonarCloud |
| `SNYK_TOKEN` | Secrets | app.snyk.io → Account settings → Auth Token |

En SonarCloud hay que **desactivar «Automatic Analysis»** del proyecto (el análisis lo hace el pipeline).

## 3. Re-test y estado de los incidentes

| Incidente | Estado en la EP3 |
| --- | --- |
| INC-05 SonarQube/Snyk en verde sin analizar | **Corregido** → re-test CP-CAL-02 y CP-SEG-12 |
| INC-13 Política `.snyk` desactualizada (nuevo) | **Corregido** |
| INC-12 Spring Boot 3.5 sin soporte OSS (nuevo, GRAVE) | Propuesta: migrar a Spring Boot 4.1 (prioridad 1 del plan) |
| INC-14 JWT en sessionStorage (nuevo, LEVE) | Riesgo aceptado para el MVP, con mitigación propuesta |
| INC-01 a INC-04 e INC-06 a INC-11 | Abiertos, con propuesta de mitigación y plazo en el plan de mantenimiento |

Los demás incidentes de la EP2 **no se corrigieron en esta entrega** por decisión del equipo; el
encargo pide *propuestas* de mitigación, que están en el informe junto con su prioridad.

## 4. Cómo ejecutar Atlas

```bash
cd App/atlas-api
cp .env.example .env            # Windows: copy .env.example .env
docker compose up --build       # http://localhost:8080/app/
```

| Recurso | URL |
| --- | --- |
| Aplicación web (EP3) | http://localhost:8080/app/ |
| Home y documentación | http://localhost:8080/ |
| Swagger UI | http://localhost:8080/swagger-ui.html |

| Usuario | Contraseña (solo ambiente QA) | Rol |
| --- | --- | --- |
| `admin` | `Admin#Atlas2026` | ADMIN |
| `editor` | `Editor#Atlas2026` | EDITOR |

Verificación rápida antes de subir cambios (pipeline local, jobs 1 a 7):

```bash
cd App/atlas-api
./mvnw clean verify             # Windows: mvnw.cmd clean verify
```
