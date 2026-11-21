package cl.duoc.atlas.e2e.support;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Crea y guarda el navegador del escenario en curso.
 *
 * <p>Con SELENIUM_URL usa un Chromium remoto (contenedor selenium/standalone-chromium); sin ella
 * usa el Chrome instalado en el equipo (Selenium Manager descarga el driver). Si ZAP_URL esta
 * definida, todo el trafico pasa por el proxy de OWASP ZAP, que asi registra cada peticion real
 * de la interfaz para el escaneo pasivo y activo.
 */
public final class Navegador {

  private static WebDriver driver;

  private Navegador() {}

  public static WebDriver actual() {
    if (driver == null) {
      throw new IllegalStateException("El navegador no fue iniciado (revise Hooks)");
    }
    return driver;
  }

  public static void iniciar() {
    ChromeOptions opciones = new ChromeOptions();
    if (Config.headless()) {
      opciones.addArguments("--headless=new");
    }
    opciones.addArguments(
        "--window-size=1366,900", "--no-sandbox", "--disable-dev-shm-usage", "--lang=es-CL");
    if (Config.zapActivo()) {
      String hostPuerto = URI.create(Config.zapUrl()).getAuthority();
      Proxy proxy = new Proxy();
      proxy.setHttpProxy(hostPuerto);
      proxy.setSslProxy(hostPuerto);
      opciones.setProxy(proxy);
      // Chrome omite el proxy para localhost salvo que se indique lo contrario.
      opciones.addArguments("--proxy-bypass-list=<-loopback>");
      opciones.setAcceptInsecureCerts(true);
    }
    if (Config.seleniumUrl().isBlank()) {
      driver = new ChromeDriver(opciones);
    } else {
      driver = new RemoteWebDriver(url(Config.seleniumUrl()), opciones);
    }
  }

  public static void cerrar() {
    if (driver != null) {
      try {
        driver.quit();
      } finally {
        driver = null;
      }
    }
  }

  private static URL url(String texto) {
    try {
      return URI.create(texto).toURL();
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("SELENIUM_URL no es una URL valida: " + texto, e);
    }
  }
}
