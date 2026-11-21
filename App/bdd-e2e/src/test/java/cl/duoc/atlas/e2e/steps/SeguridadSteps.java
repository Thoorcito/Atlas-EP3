package cl.duoc.atlas.e2e.steps;

import static org.assertj.core.api.Assertions.assertThat;

import cl.duoc.atlas.e2e.support.Config;
import cl.duoc.atlas.e2e.support.Http;
import cl.duoc.atlas.e2e.support.Navegador;
import cl.duoc.atlas.e2e.support.Ui;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Locale;

/** Steps de controles de seguridad verificados desde el navegador (uc04). */
public class SeguridadSteps {

  /** Textos que delatarian un error interno filtrado al usuario (NFR-SEG-6). */
  private static final List<String> FUGAS =
      List.of("exception", "sqlite", "sql error", "stacktrace", "whitelabel", "hibernate");

  private HttpResponse<String> respuesta;

  @Entonces("el navegador no ejecutó ningún script ni mostró alertas")
  public void sinScriptsEjecutados() {
    assertThat(Ui.huboAlertas()).as("alertas o dialogos abiertos por un script").isFalse();
    Object scriptsInyectados =
        Ui.js("return document.querySelectorAll('#tabla-clientes script').length;");
    assertThat(((Number) scriptsInyectados).intValue()).isZero();
  }

  @Entonces("la página no muestra errores internos del servidor")
  public void sinErroresInternos() {
    String pagina = Navegador.actual().getPageSource().toLowerCase(Locale.ROOT);
    assertThat(FUGAS).noneMatch(pagina::contains);
    assertThat(
            Navegador.actual().findElements(Ui.MENSAJES).stream()
                .filter(m -> m.isDisplayed() && String.valueOf(m.getDomAttribute("class")).contains("error"))
                .count())
        .as("mensajes de error visibles")
        .isZero();
  }

  @Entonces("no veo el formulario de registro ni los botones de eliminar")
  public void sinAccionesDeEscritura() {
    assertThat(Navegador.actual().findElement(Ui.SECCION_NUEVO).isDisplayed()).isFalse();
    assertThat(Navegador.actual().findElements(Ui.BTN_ELIMINAR)).isEmpty();
  }

  @Entonces("si fuerzo la creación de un cliente desde el navegador la API responde {int}")
  public void forzarCreacion(int estado) {
    Object recibido =
        Ui.jsAsincrono(
            "const listo = arguments[arguments.length - 1];"
                + "const s = JSON.parse(sessionStorage.getItem('atlas.sesion'));"
                + "fetch('/api/v1/clientes', {method: 'POST', headers: {"
                + " 'Authorization': 'Bearer ' + s.token, 'Content-Type': 'application/json'},"
                + " body: JSON.stringify({rut: '11.111.111-1', nombre: 'Forzado',"
                + " direccion: 'X 1', email: 'f@f.cl', telefono: '+56911111111'})})"
                + ".then(r => listo(r.status)).catch(() => listo(-1));");
    assertThat(((Number) recibido).intValue()).as("estado HTTP").isEqualTo(estado);
  }

  @Cuando("consulto las cabeceras de la página de inicio de sesión")
  public void consultarCabeceras() {
    respuesta = Http.get(Config.atlasUrl() + Ui.PAGINA_LOGIN, null);
    assertThat(respuesta.statusCode()).isEqualTo(200);
  }

  @Entonces("la cabecera {string} contiene {string}")
  public void cabeceraContiene(String nombre, String valor) {
    assertThat(respuesta.headers().firstValue(nombre)).as(nombre).hasValueSatisfying(
        v -> assertThat(v).contains(valor));
  }

  @Entonces("la cabecera {string} es {string}")
  public void cabeceraEs(String nombre, String valor) {
    assertThat(respuesta.headers().firstValue(nombre)).as(nombre).contains(valor);
  }
}
