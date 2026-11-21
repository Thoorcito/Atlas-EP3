package cl.duoc.atlas.e2e.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;

/**
 * Cliente minimo de la API REST de OWASP ZAP (daemon con api.disablekey=true, solo en la red
 * interna de Docker Compose). Se usa desde ZapHook al terminar la suite.
 */
public final class ZapCliente {

  private static final ObjectMapper JSON = new ObjectMapper();
  private final String base;

  public ZapCliente(String base) {
    this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
  }

  private JsonNode llamar(String ruta) {
    HttpResponse<String> r = Http.get(base + ruta, null);
    if (r.statusCode() != 200) {
      throw new IllegalStateException("ZAP respondio " + r.statusCode() + " en " + ruta);
    }
    try {
      return JSON.readTree(r.body());
    } catch (IOException e) {
      throw new IllegalStateException("Respuesta no JSON de ZAP en " + ruta, e);
    }
  }

  private static String q(String valor) {
    return URLEncoder.encode(valor, StandardCharsets.UTF_8);
  }

  public String version() {
    return llamar("/JSON/core/view/version/").path("version").asText();
  }

  /** Espera a que el escaneo pasivo procese todo el trafico registrado por el proxy. */
  public void esperarEscaneoPasivo(Duration maximo) {
    Instant limite = Instant.now().plus(maximo);
    while (Instant.now().isBefore(limite)) {
      int pendientes = llamar("/JSON/pscan/view/recordsToScan/").path("recordsToScan").asInt(0);
      if (pendientes == 0) {
        return;
      }
      pausa();
    }
  }

  /** Agrega el JWT a todas las peticiones del escaneo activo (regla del Replacer). */
  public void agregarCabeceraAutorizacion(String token) {
    llamar(
        "/JSON/replacer/action/addRule/?description="
            + q("atlas-jwt")
            + "&enabled=true&matchType=REQ_HEADER&matchRegex=false&matchString="
            + q("Authorization")
            + "&replacement="
            + q("Bearer " + token)
            + "&initiators=");
  }

  public void excluirDelEscaneo(String regex) {
    llamar("/JSON/ascan/action/excludeFromScan/?regex=" + q(regex));
    llamar("/JSON/spider/action/excludeFromScan/?regex=" + q(regex));
  }

  public void spider(String url, Duration maximo) {
    String id = llamar("/JSON/spider/action/scan/?url=" + q(url)).path("scan").asText();
    esperarEstado("/JSON/spider/view/status/?scanId=" + id, maximo);
  }

  public void escaneoActivo(String url, int minutosMaximos) {
    llamar("/JSON/ascan/action/setOptionMaxScanDurationInMins/?Integer=" + minutosMaximos);
    String id =
        llamar("/JSON/ascan/action/scan/?url=" + q(url) + "&recurse=true&inScopeOnly=false")
            .path("scan")
            .asText();
    esperarEstado(
        "/JSON/ascan/view/status/?scanId=" + id, Duration.ofMinutes(minutosMaximos + 2L));
  }

  /** Resumen High/Medium/Low/Informational de las alertas del sitio. */
  public String resumenAlertas(String url) {
    return llamar("/JSON/alert/view/alertsSummary/?baseurl=" + q(url))
        .path("alertsSummary")
        .toString();
  }

  /** Guarda el reporte HTML y las alertas en JSON en la carpeta de evidencias. */
  public void guardarReportes(String url, Path carpeta) throws IOException {
    Files.createDirectories(carpeta);
    HttpResponse<String> html = Http.get(base + "/OTHER/core/other/htmlreport/", null);
    Files.writeString(carpeta.resolve("zap-report.html"), html.body(), StandardCharsets.UTF_8);
    JsonNode alertas = llamar("/JSON/core/view/alerts/?baseurl=" + q(url));
    Files.writeString(
        carpeta.resolve("zap-alerts.json"),
        JSON.writerWithDefaultPrettyPrinter().writeValueAsString(alertas),
        StandardCharsets.UTF_8);
  }

  private void esperarEstado(String ruta, Duration maximo) {
    Instant limite = Instant.now().plus(maximo);
    while (Instant.now().isBefore(limite)) {
      if (llamar(ruta).path("status").asInt(0) >= 100) {
        return;
      }
      pausa();
    }
  }

  private static void pausa() {
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
