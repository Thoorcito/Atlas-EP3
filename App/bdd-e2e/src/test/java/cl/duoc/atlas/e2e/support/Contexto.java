package cl.duoc.atlas.e2e.support;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Estado compartido entre los steps de un escenario. Se reinicia en cada escenario (Hooks).
 *
 * <p>Los clientes que crea la suite reciben un sufijo aleatorio para que cada ejecucion sea
 * independiente de las anteriores (la base SQLite conserva los datos entre corridas).
 */
public final class Contexto {

  private static final SecureRandom AZAR = new SecureRandom();
  private static final Map<String, String> NOMBRES = new HashMap<>();
  private static String tokenGuardado;

  private Contexto() {}

  public static void reiniciar() {
    NOMBRES.clear();
    tokenGuardado = null;
  }

  /** Crea el nombre real (con sufijo) para un alias usado en el feature. */
  public static String nombreNuevo(String alias) {
    String real = alias + " " + (1000 + AZAR.nextInt(9000));
    NOMBRES.put(alias, real);
    return real;
  }

  /**
   * Usuario inexistente y unico por ejecucion, para que el bloqueo por fuerza bruta (15 minutos)
   * de una corrida no afecte a la siguiente.
   */
  public static String usuarioNuevo(String alias) {
    String real = alias + "." + (1000 + AZAR.nextInt(9000));
    NOMBRES.put(alias, real);
    return real;
  }

  /** Nombre real de un alias; si no fue creado por la suite (datos demo) es el mismo alias. */
  public static String nombreReal(String alias) {
    return NOMBRES.getOrDefault(alias, alias);
  }

  public static void guardarToken(String token) {
    tokenGuardado = token;
  }

  public static String tokenGuardado() {
    return tokenGuardado;
  }

  /** Genera un RUT chileno valido (modulo 11) con formato 12.345.678-5. */
  public static String rutValidoNuevo() {
    int cuerpo = 30_000_000 + AZAR.nextInt(9_000_000);
    return String.format(Locale.US, "%,d", cuerpo).replace(',', '.') + "-" + digitoVerificador(cuerpo);
  }

  static char digitoVerificador(int cuerpo) {
    int suma = 0;
    int factor = 2;
    for (int n = cuerpo; n > 0; n /= 10) {
      suma += (n % 10) * factor;
      factor = factor == 7 ? 2 : factor + 1;
    }
    int resto = 11 - (suma % 11);
    if (resto == 11) {
      return '0';
    }
    return resto == 10 ? 'K' : (char) ('0' + resto);
  }
}
