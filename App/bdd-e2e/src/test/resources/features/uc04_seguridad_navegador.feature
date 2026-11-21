# language: es
@uc04
Característica: Controles de seguridad verificados desde el navegador (OWASP Top 10)
  Como responsable de seguridad
  Quiero comprobar los controles con un navegador real
  Para demostrar que un atacante no puede saltarlos desde la interfaz

  @seguridad @CP-SEG-05
  Escenario: Un script en el nombre del cliente se rechaza y no se ejecuta (A03 XSS)
    Dado que inicié sesión como "admin" con la contraseña "Admin#Atlas2026"
    Cuando intento registrar un cliente con RUT "11.111.111-1", nombre "<script>alert('xss')</script>" y teléfono "+56911112222"
    Entonces el formulario muestra el error "El nombre solo admite letras"
    Y el navegador no ejecutó ningún script ni mostró alertas

  @seguridad @CP-SEG-07
  Esquema del escenario: Una inyección SQL en el buscador no expone otros clientes (A03 Inyección)
    Dado que inicié sesión como "admin" con la contraseña "Admin#Atlas2026"
    Cuando busco clientes con el texto "<carga>"
    Entonces el listado no muestra clientes
    Y la página no muestra errores internos del servidor

    Ejemplos:
      | carga                        |
      | ' OR '1'='1                  |
      | %' UNION SELECT 1,2,3 --     |

  @seguridad @CP-SEG-01
  Escenario: El editor no ve acciones de escritura y la API rechaza si las fuerza (A01)
    Dado que inicié sesión como "editor" con la contraseña "Editor#Atlas2026"
    Entonces no veo el formulario de registro ni los botones de eliminar
    Y si fuerzo la creación de un cliente desde el navegador la API responde 403

  @seguridad @CP-SEG-04
  Escenario: Sin sesión no se puede abrir el listado de clientes (A07)
    Cuando abro directamente la página de clientes sin haber iniciado sesión
    Entonces sigo en la página de inicio de sesión
    Y veo un mensaje que contiene "Debe iniciar sesión"

  @seguridad @CP-SEG-08
  Escenario: Las páginas se entregan con cabeceras de seguridad (A05)
    Cuando consulto las cabeceras de la página de inicio de sesión
    Entonces la cabecera "Content-Security-Policy" contiene "script-src 'self'"
    Y la cabecera "X-Frame-Options" es "DENY"
    Y la cabecera "X-Content-Type-Options" es "nosniff"
