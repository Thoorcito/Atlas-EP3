package cl.duoc.atlas.e2e.hooks;

import cl.duoc.atlas.e2e.support.Config;
import cl.duoc.atlas.e2e.support.Http;
import cl.duoc.atlas.e2e.support.ZapCliente;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Integracion con OWASP ZAP (DAST), igual que el lab S14 del curso.
 *
 * <ol>
 *   <li>Durante la suite, Chromium navega a traves del proxy de ZAP: el escaneo pasivo analiza
 *       cada peticion y respuesta real de la interfaz (cabeceras, cookies, divulgacion de datos).
 *   <li>Al terminar ({@code @AfterAll}) inicia sesion por la API y agrega el JWT a todas las
 *       peticiones del escaneo (regla Replacer), excluye /api/v1/auth/** para no bloquear ni
 *       revocar la cuenta, ejecuta spider + escaneo activo autenticado y guarda zap-report.html y
 *       zap-alerts.json en EVIDENCIAS_DIR/zap.
 * </ol>
 *
 * <p>Si ZAP_URL no esta definida (ejecucion local rapida) el hook no hace nada.
 */
public class ZapHook {

  @BeforeAll
  public static void verificarZap() {
    if (!Config.zapActivo()) {
      System.out.println("[ZAP] ZAP_URL no definida: la suite corre sin proxy ni escaneo.");
      return;
    }
    System.out.println("[ZAP] Proxy activo. Version de ZAP: " + cliente().version());
  }

  @AfterAll
  public static void escanear() throws IOException {
    if (!Config.zapActivo()) {
      return;
    }
    ZapCliente zap = cliente();
    String objetivo = Config.atlasUrl();
    zap.esperarEscaneoPasivo(Duration.ofMinutes(2));
    if (Config.zapEscaneoActivo()) {
      zap.excluirDelEscaneo(".*/api/v1/auth/.*");
      zap.agregarCabeceraAutorizacion(tokenAdmin());
      zap.spider(objetivo + "/app/index.html", Duration.ofMinutes(3));
      zap.escaneoActivo(objetivo, 10);
      zap.esperarEscaneoPasivo(Duration.ofMinutes(2));
    }
    System.out.println("[ZAP] Resumen de alertas: " + zap.resumenAlertas(objetivo));
    zap.guardarReportes(objetivo, Path.of(Config.carpetaEvidencias(), "zap"));
    System.out.println("[ZAP] Reportes en " + Path.of(Config.carpetaEvidencias(), "zap"));
  }

  private static ZapCliente cliente() {
    return new ZapCliente(Config.zapUrl());
  }

  private static String tokenAdmin() throws IOException {
    String cuerpo =
        "{\"nombreUsuario\":\""
            + Config.usuarioZap()
            + "\",\"contrasena\":\""
            + Config.contrasenaZap()
            + "\"}";
    HttpResponse<String> r = Http.postJson(Config.atlasUrl() + "/api/v1/auth/login", cuerpo);
    if (r.statusCode() != 200) {
      throw new IllegalStateException(
          "No se pudo iniciar sesion para el escaneo: " + r.statusCode());
    }
    return new ObjectMapper().readTree(r.body()).path("accessToken").asText();
  }
}
