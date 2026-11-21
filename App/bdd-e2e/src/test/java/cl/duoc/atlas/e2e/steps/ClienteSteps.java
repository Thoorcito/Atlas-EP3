package cl.duoc.atlas.e2e.steps;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.atlas.e2e.support.Contexto;
import cl.duoc.atlas.e2e.support.Navegador;
import cl.duoc.atlas.e2e.support.Ui;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Steps de gestion de clientes (uc02) y contratos (uc03). */
public class ClienteSteps {

  private static final String TELEFONO_VALIDO = "+56912345678";

  @Cuando("registro un cliente llamado {string} con un RUT válido nuevo")
  public void registrarClienteNuevo(String alias) {
    Ui.completarCliente(Contexto.rutValidoNuevo(), Contexto.nombreNuevo(alias), TELEFONO_VALIDO);
    Ui.guardarCliente();
  }

  @Dado("que existe un cliente registrado llamado {string}")
  public void existeCliente(String alias) {
    registrarClienteNuevo(alias);
    Ui.esperarMensajeQueContiene("registrado correctamente");
  }

  @Cuando("intento registrar un cliente con RUT {string}, nombre {string} y teléfono {string}")
  public void intentarRegistrar(String rut, String nombre, String telefono) {
    Ui.vigilarAlertas();
    Ui.completarCliente(rut, nombre, telefono);
    Ui.guardarCliente();
  }

  @Entonces("el formulario muestra el error {string}")
  public void formularioMuestraError(String texto) {
    Ui.espera()
        .withMessage("El formulario no mostro el error: " + texto)
        .until(d -> Ui.textoFormulario().contains(texto));
  }

  @Cuando("busco clientes con el texto {string}")
  public void buscar(String texto) {
    Ui.buscar(texto);
  }

  @Entonces("el listado muestra solo {int} cliente(s)")
  public void listadoMuestra(int cantidad) {
    assertThat(Ui.filasClientes()).hasSize(cantidad);
  }

  @Entonces("el listado no muestra clientes")
  public void listadoVacio() {
    assertThat(Ui.filasClientes()).isEmpty();
    assertThat(Navegador.actual().findElement(Ui.SIN_RESULTADOS).isDisplayed()).isTrue();
  }

  @Entonces("el cliente {string} aparece en el listado")
  public void clienteAparece(String alias) {
    String nombre = Contexto.nombreReal(alias);
    Ui.buscar(nombre);
    assertThat(Ui.filaCliente(nombre)).as("fila del cliente " + nombre).isPresent();
  }

  @Entonces("el cliente {string} ya no aparece en el listado")
  public void clienteNoAparece(String alias) {
    String nombre = Contexto.nombreReal(alias);
    Ui.buscar(nombre);
    assertThat(Ui.filaCliente(nombre)).as("fila del cliente " + nombre).isEmpty();
  }

  @Cuando("pido eliminar el cliente {string} y cancelo la confirmación")
  public void eliminarYCancelar(String alias) {
    String nombre = Contexto.nombreReal(alias);
    Ui.buscar(nombre);
    Ui.pedirEliminar(nombre);
    Ui.clic(Ui.BTN_CANCELAR);
    Ui.espera().until(ExpectedConditions.invisibilityOfElementLocated(Ui.DIALOGO));
  }

  @Cuando("pido eliminar el cliente {string} y confirmo")
  public void eliminarYConfirmar(String alias) {
    String nombre = Contexto.nombreReal(alias);
    Ui.buscar(nombre);
    Ui.pedirEliminar(nombre);
    Ui.limpiarMensajes();
    Ui.clic(Ui.BTN_CONFIRMAR);
    Ui.esperarMensajeQueContiene("eliminado");
  }

  @Cuando("abro los contratos del cliente {string}")
  public void abrirContratos(String nombre) {
    Ui.buscar(nombre);
    Ui.filaCliente(nombre)
        .orElseThrow(() -> new AssertionError("No se encontro el cliente " + nombre))
        .findElement(Ui.VER_CONTRATOS)
        .click();
    Ui.esperarUrl("/app/contratos.html");
    Ui.esperarCarga();
  }

  @Entonces("el contrato {string} tiene el estado {string}")
  public void contratoTieneEstado(String titulo, String estado) {
    assertThat(Ui.estadoContrato(titulo)).isEqualTo(estado);
  }
}
