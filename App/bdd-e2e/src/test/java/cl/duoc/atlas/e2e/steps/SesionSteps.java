package cl.duoc.atlas.e2e.steps;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.atlas.e2e.support.Config;
import cl.duoc.atlas.e2e.support.Contexto;
import cl.duoc.atlas.e2e.support.Http;
import cl.duoc.atlas.e2e.support.Ui;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import org.openqa.selenium.support.ui.ExpectedConditions;

/** Steps de inicio y cierre de sesion (uc01) y acceso sin sesion (uc04). */
public class SesionSteps {

  @Dado("que abro la página de inicio de sesión")
  public void abrirLogin() {
    Ui.abrir(Ui.PAGINA_LOGIN);
    Ui.espera().until(ExpectedConditions.visibilityOfElementLocated(Ui.USUARIO));
  }

  @Cuando("inicio sesión como {string} con la contraseña {string}")
  public void iniciarSesion(String usuario, String contrasena) {
    Ui.login(Contexto.nombreReal(usuario), contrasena);
  }

  @Dado("que inicié sesión como {string} con la contraseña {string}")
  public void sesionIniciada(String usuario, String contrasena) {
    abrirLogin();
    Ui.login(usuario, contrasena);
    veoPaginaClientes();
  }

  @Entonces("veo la página de clientes")
  public void veoPaginaClientes() {
    Ui.esperarUrl(Ui.PAGINA_CLIENTES);
    Ui.esperarCarga();
  }

  @Entonces("el encabezado muestra el rol {string}")
  public void encabezadoMuestraRol(String rol) {
    assertThat(Ui.espera().until(d -> d.findElement(Ui.ROL_ACTUAL).getText())).isEqualTo(rol);
  }

  @Entonces("veo el mensaje {string}")
  public void veoMensaje(String texto) {
    Ui.esperarMensajeQueContiene(texto);
  }

  @Entonces("veo un mensaje que contiene {string}")
  public void veoMensajeQueContiene(String texto) {
    Ui.esperarMensajeQueContiene(texto);
  }

  @Entonces("sigo en la página de inicio de sesión")
  public void sigoEnLogin() {
    Ui.esperarUrl(Ui.PAGINA_LOGIN);
    assertThat(Ui.urlActual()).doesNotContain(Ui.PAGINA_CLIENTES);
  }

  @Cuando("fallo el inicio de sesión {int} veces con el usuario {string}")
  public void falloVariasVeces(int veces, String usuario) {
    String real = Contexto.usuarioNuevo(usuario);
    for (int i = 1; i <= veces; i++) {
      Ui.login(real, "Incorrecta#" + i);
      Ui.esperarMensajeQueContiene("incorrectos");
    }
  }

  @Cuando("cierro la sesión")
  public void cerrarSesion() {
    Contexto.guardarToken(Ui.tokenActual());
    assertThat(Contexto.tokenGuardado()).as("token en sessionStorage").isNotBlank();
    Ui.clic(Ui.BTN_SALIR);
    Ui.esperarUrl("motivo=salida");
  }

  @Entonces("al abrir directamente la página de clientes vuelvo al inicio de sesión")
  public void abrirClientesTrasSalir() {
    Ui.abrir(Ui.PAGINA_CLIENTES);
    Ui.esperarUrl("motivo=sin-sesion");
  }

  @Entonces("el token anterior es rechazado por la API con estado {int}")
  public void tokenRechazado(int estado) {
    int recibido =
        Http.get(Config.atlasUrl() + "/api/v1/clientes", Contexto.tokenGuardado()).statusCode();
    assertThat(recibido).as("estado HTTP con el token revocado").isEqualTo(estado);
  }

  @Cuando("abro directamente la página de clientes sin haber iniciado sesión")
  public void abrirClientesSinSesion() {
    Ui.abrir(Ui.PAGINA_CLIENTES);
    Ui.esperarUrl("motivo=sin-sesion");
  }
}
