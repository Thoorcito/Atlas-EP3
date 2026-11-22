# language: es
@uc03
Característica: Contratos del cliente con su estado (RF-5.1, RF-5.4, RB-3)
  Como editor
  Quiero ver los contratos de un cliente con su estado
  Para anticipar las renovaciones

  @funcional @CP-FUN-02 @CP-INT-02
  Escenario: El estado de cada contrato se calcula con la fecha de término
    Dado que inicié sesión como "editor" con la contraseña "Editor#Atlas2026"
    Cuando abro los contratos del cliente "Ferretería Los Andes"
    Entonces el contrato "Suministro anual" tiene el estado "ACTIVO"
    Y el contrato "Mantención de bodega" tiene el estado "POR VENCER"
