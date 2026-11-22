# language: es
@uc02
Característica: Gestión de clientes desde el navegador (RF-4.1, RF-4.2, RF-4.3)
  Como administrador
  Quiero registrar, buscar y eliminar clientes
  Para mantener actualizada la cartera de la empresa

  Antecedentes:
    Dado que inicié sesión como "admin" con la contraseña "Admin#Atlas2026"

  @funcional @CP-FUN-06
  Escenario: Registrar un cliente con un RUT válido
    Cuando registro un cliente llamado "Comercial E2E" con un RUT válido nuevo
    Entonces veo un mensaje que contiene "registrado correctamente"
    Y el cliente "Comercial E2E" aparece en el listado

  @funcional @CP-FUN-08
  Esquema del escenario: Rechazar datos inválidos con un mensaje claro en español
    Cuando intento registrar un cliente con RUT "<rut>", nombre "<nombre>" y teléfono "<telefono>"
    Entonces el formulario muestra el error "<mensaje>"

    Ejemplos:
      | rut          | nombre           | telefono     | mensaje                                     |
      | 12.345.678-9 | Cliente RUT Malo | +56911112222 | dígito verificador incorrecto               |
      | 12.345.678-5 | Cliente Fono     | 123          | El teléfono debe tener entre 8 y 12 dígitos |

  @funcional @CP-FUN-10
  Escenario: Buscar clientes por nombre
    Cuando busco clientes con el texto "Espiga"
    Entonces el listado muestra solo 1 cliente
    Y el cliente "Panadería La Espiga" aparece en el listado

  @funcional @CP-FUN-01
  Escenario: La eliminación exige confirmación y es lógica
    Dado que existe un cliente registrado llamado "Cliente Para Eliminar"
    Cuando pido eliminar el cliente "Cliente Para Eliminar" y cancelo la confirmación
    Entonces el cliente "Cliente Para Eliminar" aparece en el listado
    Cuando pido eliminar el cliente "Cliente Para Eliminar" y confirmo
    Entonces veo un mensaje que contiene "eliminado correctamente"
    Y el cliente "Cliente Para Eliminar" ya no aparece en el listado
