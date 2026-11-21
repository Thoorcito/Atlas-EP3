/* Atlas - pagina de inicio de sesion (RF-1.1). */
"use strict";

(() => {
  const form = document.getElementById("form-login");
  const mensaje = document.getElementById("mensaje");
  const boton = document.getElementById("btn-ingresar");

  const MOTIVOS = {
    "sin-sesion": "Debe iniciar sesión para continuar.",
    expirada: "Su sesión expiró o fue cerrada. Inicie sesión nuevamente.",
    salida: "Sesión cerrada correctamente.",
  };

  const motivo = new URLSearchParams(window.location.search).get("motivo");
  if (motivo && Object.prototype.hasOwnProperty.call(MOTIVOS, motivo)) {
    AtlasApi.mostrarMensaje(mensaje, MOTIVOS[motivo], motivo === "salida" ? "exito" : "info");
  }

  // Si ya hay una sesion abierta, ir directo al listado.
  if (AtlasApi.sesion() !== null && motivo === null) {
    window.location.replace("/app/clientes.html");
  }

  form.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    const nombreUsuario = document.getElementById("usuario").value.trim();
    const contrasena = document.getElementById("contrasena").value;
    if (!nombreUsuario || !contrasena) {
      AtlasApi.mostrarMensaje(mensaje, "Ingrese su usuario y su contraseña.", "error");
      return;
    }
    boton.disabled = true;
    const resultado = await AtlasApi.pedir("POST", "/auth/login", { nombreUsuario, contrasena });
    boton.disabled = false;
    if (resultado.ok) {
      AtlasApi.guardarSesion(resultado.datos);
      window.location.replace("/app/clientes.html");
      return;
    }
    document.getElementById("contrasena").value = "";
    AtlasApi.mostrarMensaje(mensaje, AtlasApi.mensajeError(resultado), "error");
  });
})();
