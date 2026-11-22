# language: es
@uc01
Característica: Inicio y cierre de sesión desde el navegador (RF-1.1, RF-1.2, NFR-SEG-9)
  Como usuario interno de una pyme
  Quiero iniciar y cerrar sesión de forma segura
  Para que solo personas autorizadas vean los datos de los clientes

  @funcional @CP-FUN-03
  Escenario: El administrador inicia sesión y ve el listado de clientes
    Dado que abro la página de inicio de sesión
    Cuando inicio sesión como "admin" con la contraseña "Admin#Atlas2026"
    Entonces veo la página de clientes
    Y el encabezado muestra el rol "ADMIN"

  @seguridad @CP-SEG-02
  Escenario: Credenciales incorrectas muestran un mensaje genérico
    Dado que abro la página de inicio de sesión
    Cuando inicio sesión como "admin" con la contraseña "ClaveIncorrecta#1"
    Entonces veo el mensaje "Usuario o contraseña incorrectos"
    Y sigo en la página de inicio de sesión

  @seguridad @CP-SEG-03
  Escenario: Cinco intentos fallidos bloquean temporalmente la cuenta
    Dado que abro la página de inicio de sesión
    Cuando fallo el inicio de sesión 5 veces con el usuario "intruso.e2e"
    Y inicio sesión como "intruso.e2e" con la contraseña "OtraClave#2"
    Entonces veo un mensaje que contiene "Demasiados intentos fallidos"

  @seguridad @CP-FUN-04
  Escenario: Después de cerrar sesión no se puede volver al listado
    Dado que inicié sesión como "admin" con la contraseña "Admin#Atlas2026"
    Cuando cierro la sesión
    Entonces veo un mensaje que contiene "Sesión cerrada"
    Y al abrir directamente la página de clientes vuelvo al inicio de sesión
    Y el token anterior es rechazado por la API con estado 401
