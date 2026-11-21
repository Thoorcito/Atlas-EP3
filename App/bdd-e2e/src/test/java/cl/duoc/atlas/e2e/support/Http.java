package cl.duoc.atlas.e2e.support;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP directo (sin navegador) para lo que Selenium no puede ver: cabeceras de respuesta,
 * estado HTTP de un token revocado y la API de OWASP ZAP.
 */
public final class Http {

  private static final HttpClient CLIENTE =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  private Http() {}

  public static HttpResponse<String> get(String url, String bearer) {
    HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30));
    if (bearer != null) {
      b.header("Authorization", "Bearer " + bearer);
    }
    return enviar(b.GET().build());
  }

  public static HttpResponse<String> postJson(String url, String json) {
    HttpRequest req =
        HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
    return enviar(req);
  }

  private static HttpResponse<String> enviar(HttpRequest req) {
    try {
      return CLIENTE.send(req, HttpResponse.BodyHandlers.ofString());
    } catch (IOException e) {
      throw new IllegalStateException("No se pudo conectar con " + req.uri(), e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Peticion interrumpida: " + req.uri(), e);
    }
  }
}
