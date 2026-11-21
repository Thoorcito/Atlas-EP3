package cl.duoc.atlas.e2e.support;

/**
 * Configuracion de la suite E2E. Cada valor se lee primero como propiedad del sistema
 * (-Datlas.url=...) y luego como variable de entorno (ATLAS_URL=...), con un valor por defecto
 * pensado para ejecutar en local sin Docker.
 *
 * <ul>
 *   <li>ATLAS_URL: URL de la aplicacion. En Docker Compose es http://atlas-api:8080.
 *   <li>SELENIUM_URL: Selenium remoto (http://selenium:4444). Vacio = Chrome local.
 *   <li>ZAP_URL: API de OWASP ZAP (http://zap:8090). Vacio = sin proxy ni escaneo.
 *   <li>ZAP_ESCANEO_ACTIVO: true para ejecutar el escaneo activo al final de la suite.
 *   <li>HEADLESS: false para ver el navegador al ejecutar en local.
 * </ul>
 */
public final class Config {

  private Config() {}

  private static String valor(String propiedad, String variable, String porDefecto) {
    String v = System.getProperty(propiedad);
    if (v == null || v.isBlank()) {
      v = System.getenv(variable);
    }
    return v == null || v.isBlank() ? porDefecto : v.trim();
  }

  public static String atlasUrl() {
    String url = valor("atlas.url", "ATLAS_URL", "http://localhost:8080");
    return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
  }

  public static String seleniumUrl() {
    return valor("selenium.url", "SELENIUM_URL", "");
  }

  public static String zapUrl() {
    return valor("zap.url", "ZAP_URL", "");
  }

  public static boolean zapActivo() {
    return !zapUrl().isBlank();
  }

  public static boolean zapEscaneoActivo() {
    return Boolean.parseBoolean(valor("zap.escaneo.activo", "ZAP_ESCANEO_ACTIVO", "true"));
  }

  public static boolean headless() {
    return Boolean.parseBoolean(valor("headless", "HEADLESS", "true"));
  }

  /** Carpeta donde se guardan las capturas y los reportes de ZAP. */
  public static String carpetaEvidencias() {
    return valor("evidencias.dir", "EVIDENCIAS_DIR", "target/evidencias");
  }

  /** Credenciales que usa ZapHook para autenticar el escaneo activo. */
  public static String usuarioZap() {
    return valor("zap.usuario", "ZAP_USUARIO", "admin");
  }

  public static String contrasenaZap() {
    return valor("zap.contrasena", "ZAP_CONTRASENA", "Admin#Atlas2026");
  }
}
