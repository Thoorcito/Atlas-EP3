/*
 * Atlas - frontend minimo (EP3).
 * Cliente HTTP comun para todas las paginas.
 *
 * Decisiones de seguridad (ver informe EP3, seccion 3):
 *  - El JWT se guarda en sessionStorage: se borra al cerrar la pestana y no viaja en cookies,
 *    por lo que no hay riesgo de CSRF (el navegador no lo envia solo desde otro sitio).
 *  - El riesgo de sessionStorage es el XSS; se mitiga con la CSP "script-src 'self'" (sin
 *    scripts en linea), con la validacion del backend (rechaza < y >) y porque la interfaz
 *    escribe los datos SIEMPRE con textContent, nunca con innerHTML.
 *  - Ante un 401 (token vencido o revocado) se borra la sesion y se vuelve al login.
 */
"use strict";

const AtlasApi = (() => {
  const CLAVE_SESION = "atlas.sesion";
  const BASE = "/api/v1";

  function guardarSesion(tokenResponse) {
    sessionStorage.setItem(
      CLAVE_SESION,
      JSON.stringify({
        token: tokenResponse.accessToken,
        usuario: tokenResponse.usuario,
        rol: tokenResponse.rol,
        expiraEn: tokenResponse.expiraEn,
      })
    );
  }

  function sesion() {
    try {
      const valor = sessionStorage.getItem(CLAVE_SESION);
      return valor ? JSON.parse(valor) : null;
    } catch (e) {
      return null;
    }
  }

  function borrarSesion() {
    sessionStorage.removeItem(CLAVE_SESION);
  }

  function esAdmin() {
    const s = sesion();
    return s !== null && s.rol === "ADMIN";
  }

  /** Redirige al login si no hay sesion. Se llama al cargar las paginas protegidas. */
  function exigirSesion() {
    if (sesion() === null) {
      window.location.replace("/app/index.html?motivo=sin-sesion");
      return false;
    }
    return true;
  }

  /**
   * Ejecuta una peticion a la API. Devuelve { ok, status, datos }.
   * Nunca lanza excepciones hacia la interfaz: los errores se muestran como mensajes.
   */
  async function pedir(metodo, ruta, cuerpo) {
    const cabeceras = { Accept: "application/json" };
    const s = sesion();
    if (s !== null) {
      cabeceras.Authorization = "Bearer " + s.token;
    }
    if (cuerpo !== undefined) {
      cabeceras["Content-Type"] = "application/json";
    }
    let respuesta;
    try {
      respuesta = await fetch(BASE + ruta, {
        method: metodo,
        headers: cabeceras,
        body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
        credentials: "omit",
        cache: "no-store",
      });
    } catch (e) {
      return { ok: false, status: 0, datos: { message: "No fue posible conectar con el servidor." } };
    }
    let datos = null;
    const texto = await respuesta.text();
    if (texto) {
      try {
        datos = JSON.parse(texto);
      } catch (e) {
        datos = null;
      }
    }
    if (respuesta.status === 401 && ruta !== "/auth/login") {
      borrarSesion();
      window.location.replace("/app/index.html?motivo=expirada");
    }
    return { ok: respuesta.ok, status: respuesta.status, datos: datos };
  }

  /** Mensaje legible de un error de la API (ErrorResponse del backend). */
  function mensajeError(resultado) {
    if (resultado.datos && resultado.datos.message) {
      return resultado.datos.message;
    }
    if (resultado.status === 403) {
      return "No tiene permisos para realizar esta acción.";
    }
    return "Ocurrió un error inesperado. Intente nuevamente.";
  }

  /** Escribe un mensaje en un contenedor con role="alert" (lectores de pantalla lo anuncian). */
  function mostrarMensaje(elemento, texto, tipo) {
    elemento.textContent = texto;
    elemento.className = "mensaje " + (tipo || "error");
    elemento.hidden = !texto;
  }

  /** Crea una celda de tabla con texto plano (proteccion contra XSS). */
  function celda(texto, clase) {
    const td = document.createElement("td");
    td.textContent = texto === null || texto === undefined ? "" : String(texto);
    if (clase) {
      td.className = clase;
    }
    return td;
  }

  /** Encabezado comun: usuario, rol y boton de salida. */
  function iniciarEncabezado() {
    const s = sesion();
    if (s === null) {
      return;
    }
    document.getElementById("usuario-actual").textContent = s.usuario;
    document.getElementById("rol-actual").textContent = s.rol;
    document.getElementById("btn-salir").addEventListener("click", async () => {
      await pedir("POST", "/auth/logout");
      borrarSesion();
      window.location.replace("/app/index.html?motivo=salida");
    });
  }

  return {
    guardarSesion,
    sesion,
    borrarSesion,
    esAdmin,
    exigirSesion,
    pedir,
    mensajeError,
    mostrarMensaje,
    celda,
    iniciarEncabezado,
  };
})();
