package cl.duoc.atlas.e2e.hooks;

import cl.duoc.atlas.e2e.support.Config;
import cl.duoc.atlas.e2e.support.Contexto;
import cl.duoc.atlas.e2e.support.Navegador;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.Locale;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

/**
 * Abre un navegador limpio por escenario y, al terminar, guarda una captura como evidencia.
 *
 * <p>Nombre de la captura: {@code <uc>_<escenario>_<OK|FALLA>.png} en EVIDENCIAS_DIR/capturas. La
 * captura tambien se adjunta al reporte HTML de Cucumber.
 */
public class Hooks {

  @Before
  public void abrirNavegador() {
    Contexto.reiniciar();
    Navegador.iniciar();
  }

  @After
  public void capturarYCerrar(Scenario escenario) throws IOException {
    try {
      byte[] png = ((TakesScreenshot) Navegador.actual()).getScreenshotAs(OutputType.BYTES);
      escenario.attach(png, "image/png", escenario.getName());
      Path carpeta = Path.of(Config.carpetaEvidencias(), "capturas");
      Files.createDirectories(carpeta);
      Files.write(carpeta.resolve(nombreCaptura(escenario)), png);
    } finally {
      Navegador.cerrar();
    }
  }

  private static String nombreCaptura(Scenario escenario) {
    String uc =
        escenario.getSourceTagNames().stream()
            .filter(t -> t.startsWith("@uc"))
            .findFirst()
            .orElse("@uc")
            .substring(1);
    String caso =
        escenario.getSourceTagNames().stream()
            .filter(t -> t.startsWith("@CP-"))
            .findFirst()
            .map(t -> "_" + t.substring(1))
            .orElse("");
    String nombre =
        Normalizer.normalize(escenario.getName(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
    if (nombre.length() > 60) {
      nombre = nombre.substring(0, 60);
    }
    String linea = "_l" + escenario.getLine();
    String resultado = escenario.isFailed() ? "FALLA" : "OK";
    return uc + caso + "_" + nombre + linea + "_" + resultado + ".png";
  }
}
