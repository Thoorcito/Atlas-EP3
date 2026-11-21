package cl.duoc.atlas.e2e.support;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Selectores y acciones de la interfaz de Atlas, en un solo lugar (patron del lab S14).
 *
 * <p>Los steps expresan la intencion del negocio; si cambia el HTML solo se ajusta esta clase.
 * Todas las esperas son explicitas (WebDriverWait), nunca Thread.sleep.
 */
public final class Ui {

  // ---- Paginas ----
  public static final String PAGINA_LOGIN = "/app/index.html";
  public static final String PAGINA_CLIENTES = "/app/clientes.html";

  // ---- Login ----
  public static final By USUARIO = By.id("usuario");
  public static final By CONTRASENA = By.id("contrasena");
  public static final By BTN_INGRESAR = By.id("btn-ingresar");

  // ---- Encabezado ----
  public static final By ROL_ACTUAL = By.id("rol-actual");
  public static final By BTN_SALIR = By.id("btn-salir");

  // ---- Clientes ----
  public static final By FILTRO = By.id("filtro");
  public static final By BTN_BUSCAR = By.id("btn-buscar");
  public static final By FILAS_CLIENTES = By.cssSelector("#tabla-clientes tbody tr");
  public static final By SIN_RESULTADOS = By.id("sin-resultados");
  public static final By SECCION_NUEVO = By.id("seccion-nuevo");
  public static final By MENSAJE_FORM = By.id("mensaje-form");
  public static final By BTN_GUARDAR = By.id("btn-guardar");
  public static final By BTN_ELIMINAR = By.cssSelector(".btn-eliminar");
  public static final By VER_CONTRATOS = By.cssSelector(".ver-contratos");
  public static final By DIALOGO = By.cssSelector("#dialogo-confirmar[open]");
  public static final By BTN_CONFIRMAR = By.id("btn-confirmar-eliminar");
  public static final By BTN_CANCELAR = By.id("btn-cancelar-eliminar");
  public static final By MENSAJES = By.cssSelector(".mensaje");

  // ---- Contratos ----
  public static final By FILAS_CONTRATOS = By.cssSelector("#tabla-contratos tbody tr");

  private Ui() {}

  private static WebDriver d() {
    return Navegador.actual();
  }

  public static WebDriverWait espera() {
    return new WebDriverWait(d(), Duration.ofSeconds(15));
  }

  public static void abrir(String ruta) {
    d().get(Config.atlasUrl() + ruta);
  }

  public static void escribir(By campo, String texto) {
    WebElement e = espera().until(ExpectedConditions.visibilityOfElementLocated(campo));
    e.clear();
    e.sendKeys(texto);
  }

  public static void clic(By elemento) {
    espera().until(ExpectedConditions.elementToBeClickable(elemento)).click();
  }

  public static String urlActual() {
    return d().getCurrentUrl();
  }

  public static void esperarUrl(String fragmento) {
    espera().until(ExpectedConditions.urlContains(fragmento));
  }

  public static Object js(String script, Object... args) {
    return ((JavascriptExecutor) d()).executeScript(script, args);
  }

  public static Object jsAsincrono(String script, Object... args) {
    return ((JavascriptExecutor) d()).executeAsyncScript(script, args);
  }

  /** Espera a que la pagina termine de cargar datos de la API (atributo data-cargando). */
  public static void esperarCarga() {
    espera()
        .until(
            drv ->
                "false"
                    .equals(
                        ((JavascriptExecutor) drv)
                            .executeScript("return document.body.dataset.cargando;")));
  }

  // ---- Acciones de negocio ----

  public static void login(String usuario, String contrasena) {
    escribir(USUARIO, usuario);
    escribir(CONTRASENA, contrasena);
    limpiarMensajes();
    clic(BTN_INGRESAR);
  }

  /** Oculta los mensajes previos para que la siguiente espera solo vea mensajes nuevos. */
  public static void limpiarMensajes() {
    js(
        "document.querySelectorAll('.mensaje').forEach(m => { m.hidden = true;"
            + " m.textContent = ''; });");
  }

  /** Espera hasta que algun mensaje visible contenga el texto indicado. */
  public static void esperarMensajeQueContiene(String texto) {
    espera()
        .withMessage("No aparecio un mensaje que contenga: " + texto)
        .until(
            drv ->
                drv.findElements(MENSAJES).stream()
                    .anyMatch(m -> m.isDisplayed() && m.getText().contains(texto)));
  }

  public static void buscar(String texto) {
    escribir(FILTRO, texto);
    clic(BTN_BUSCAR);
    esperarCarga();
  }

  public static List<WebElement> filasClientes() {
    return d().findElements(FILAS_CLIENTES);
  }

  public static Optional<WebElement> filaCliente(String nombre) {
    return filasClientes().stream()
        .filter(f -> f.findElement(By.cssSelector(".col-nombre")).getText().equals(nombre))
        .findFirst();
  }

  public static void completarCliente(String rut, String nombre, String telefono) {
    escribir(By.id("rut"), rut);
    escribir(By.id("nombre"), nombre);
    escribir(By.id("email"), "e2e@atlas.cl");
    escribir(By.id("telefono"), telefono);
    escribir(By.id("direccion"), "Av. Prueba 123, Santiago");
    escribir(By.id("descripcion"), "Creado por la suite E2E");
  }

  /** Envia el formulario y espera la respuesta (mensaje de exito o de error). */
  public static void guardarCliente() {
    limpiarMensajes();
    clic(BTN_GUARDAR);
    espera().until(ExpectedConditions.visibilityOfElementLocated(MENSAJE_FORM));
  }

  public static String textoFormulario() {
    return d().findElement(SECCION_NUEVO).getText();
  }

  public static void pedirEliminar(String nombre) {
    WebElement fila =
        filaCliente(nombre)
            .orElseThrow(() -> new AssertionError("No se encontro el cliente " + nombre));
    fila.findElement(BTN_ELIMINAR).click();
    espera().until(ExpectedConditions.visibilityOfElementLocated(DIALOGO));
  }

  public static String estadoContrato(String titulo) {
    return d().findElements(FILAS_CONTRATOS).stream()
        .filter(f -> f.findElement(By.cssSelector(".col-titulo")).getText().equals(titulo))
        .map(f -> f.findElement(By.cssSelector(".col-estado")).getText())
        .findFirst()
        .orElseThrow(() -> new AssertionError("No se encontro el contrato " + titulo));
  }

  /** Reemplaza alert/confirm/prompt para registrar si algun script intenta usarlos. */
  public static void vigilarAlertas() {
    js(
        "window.__alertas = [];"
            + "['alert','confirm','prompt'].forEach(f => { window[f] = (m) => {"
            + " window.__alertas.push(String(m)); return null; }; });");
  }

  public static boolean huboAlertas() {
    try {
      d().switchTo().alert();
      return true;
    } catch (NoAlertPresentException e) {
      Object registradas = js("return (window.__alertas || []).length;");
      return registradas instanceof Number n && n.intValue() > 0;
    }
  }

  /** JWT de la sesion actual (sessionStorage), para verificar la revocacion contra la API. */
  public static String tokenActual() {
    Object t =
        js(
            "const s = sessionStorage.getItem('atlas.sesion');"
                + " return s ? JSON.parse(s).token : null;");
    return t == null ? null : t.toString();
  }
}
