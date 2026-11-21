/* Atlas - listado, busqueda, registro y eliminacion de clientes (RF-4.1, RF-4.2, RF-4.3). */
"use strict";

(() => {
  if (!AtlasApi.exigirSesion()) {
    return;
  }
  AtlasApi.iniciarEncabezado();

  const TAMANO = 10;
  const CAMPOS = ["rut", "nombre", "email", "telefono", "direccion", "descripcion"];
  const mensaje = document.getElementById("mensaje");
  const mensajeForm = document.getElementById("mensaje-form");
  const cuerpoTabla = document.querySelector("#tabla-clientes tbody");
  const sinResultados = document.getElementById("sin-resultados");
  const dialogo = document.getElementById("dialogo-confirmar");
  const admin = AtlasApi.esAdmin();

  let pagina = 0;
  let totalPaginas = 0;
  let filtro = "";
  let idPorEliminar = null;

  if (admin) {
    document.getElementById("seccion-nuevo").hidden = false;
  }

  function filaCliente(cliente) {
    const tr = document.createElement("tr");
    tr.dataset.id = String(cliente.id);
    tr.appendChild(AtlasApi.celda(cliente.nombre, "col-nombre"));
    tr.appendChild(AtlasApi.celda(cliente.rut, "col-rut"));
    tr.appendChild(AtlasApi.celda(cliente.email, "col-email"));
    tr.appendChild(AtlasApi.celda(cliente.telefono, "col-telefono"));

    const celdaAcciones = document.createElement("td");
    const acciones = document.createElement("div");
    acciones.className = "acciones";
    celdaAcciones.appendChild(acciones);
    const ver = document.createElement("a");
    ver.className = "boton enlace ver-contratos";
    ver.href = "/app/contratos.html?cliente=" + encodeURIComponent(cliente.id);
    ver.textContent = "Ver contratos";
    acciones.appendChild(ver);

    if (admin) {
      const eliminar = document.createElement("button");
      eliminar.type = "button";
      eliminar.className = "boton peligro btn-eliminar";
      eliminar.textContent = "Eliminar";
      eliminar.setAttribute("aria-label", "Eliminar a " + cliente.nombre);
      eliminar.addEventListener("click", () => pedirConfirmacion(cliente));
      acciones.appendChild(eliminar);
    }
    tr.appendChild(celdaAcciones);
    return tr;
  }

  // data-cargando permite a las pruebas E2E (Selenium) esperar a que termine cada carga.
  async function cargar() {
    document.body.dataset.cargando = "true";
    const ruta =
      "/clientes?nombre=" +
      encodeURIComponent(filtro) +
      "&pagina=" +
      pagina +
      "&tamano=" +
      TAMANO;
    const resultado = await AtlasApi.pedir("GET", ruta);
    cuerpoTabla.replaceChildren();
    if (!resultado.ok) {
      AtlasApi.mostrarMensaje(mensaje, AtlasApi.mensajeError(resultado), "error");
      document.body.dataset.cargando = "false";
      return;
    }
    const datos = resultado.datos;
    datos.contenido.forEach((cliente) => cuerpoTabla.appendChild(filaCliente(cliente)));
    totalPaginas = datos.totalPaginas;
    sinResultados.hidden = datos.contenido.length > 0;
    document.getElementById("info-pagina").textContent =
      totalPaginas === 0 ? "" : "Página " + (pagina + 1) + " de " + totalPaginas;
    document.getElementById("btn-anterior").disabled = pagina === 0;
    document.getElementById("btn-siguiente").disabled = pagina + 1 >= totalPaginas;
    document.body.dataset.cargando = "false";
  }

  document.getElementById("form-buscar").addEventListener("submit", (evento) => {
    evento.preventDefault();
    filtro = document.getElementById("filtro").value.trim();
    pagina = 0;
    AtlasApi.mostrarMensaje(mensaje, "", "info");
    cargar();
  });

  document.getElementById("btn-anterior").addEventListener("click", () => {
    if (pagina > 0) {
      pagina -= 1;
      cargar();
    }
  });

  document.getElementById("btn-siguiente").addEventListener("click", () => {
    if (pagina + 1 < totalPaginas) {
      pagina += 1;
      cargar();
    }
  });

  // ---- Registro de clientes (solo ADMIN) ----
  function limpiarErrores() {
    CAMPOS.forEach((campo) => {
      document.getElementById("error-" + campo).textContent = "";
      document.getElementById(campo).removeAttribute("aria-invalid");
    });
  }

  function mostrarErroresCampo(detalles) {
    Object.keys(detalles || {}).forEach((campo) => {
      const destino = document.getElementById("error-" + campo);
      if (destino) {
        destino.textContent = detalles[campo];
        document.getElementById(campo).setAttribute("aria-invalid", "true");
      }
    });
  }

  document.getElementById("form-cliente").addEventListener("submit", async (evento) => {
    evento.preventDefault();
    limpiarErrores();
    const cuerpo = {};
    CAMPOS.forEach((campo) => {
      cuerpo[campo] = document.getElementById(campo).value.trim();
    });
    if (!cuerpo.descripcion) {
      delete cuerpo.descripcion;
    }
    const boton = document.getElementById("btn-guardar");
    boton.disabled = true;
    const resultado = await AtlasApi.pedir("POST", "/clientes", cuerpo);
    boton.disabled = false;
    if (resultado.ok) {
      document.getElementById("form-cliente").reset();
      AtlasApi.mostrarMensaje(
        mensajeForm,
        "Cliente «" + resultado.datos.nombre + "» registrado correctamente.",
        "exito"
      );
      filtro = "";
      document.getElementById("filtro").value = "";
      pagina = 0;
      cargar();
      return;
    }
    AtlasApi.mostrarMensaje(mensajeForm, AtlasApi.mensajeError(resultado), "error");
    if (resultado.datos) {
      mostrarErroresCampo(resultado.datos.details);
    }
  });

  // ---- Eliminacion logica con confirmacion (solo ADMIN) ----
  function pedirConfirmacion(cliente) {
    idPorEliminar = cliente.id;
    document.getElementById("nombre-eliminar").textContent = cliente.nombre;
    dialogo.showModal();
  }

  document.getElementById("btn-cancelar-eliminar").addEventListener("click", () => {
    idPorEliminar = null;
    dialogo.close();
  });

  document.getElementById("btn-confirmar-eliminar").addEventListener("click", async () => {
    const id = idPorEliminar;
    dialogo.close();
    if (id === null) {
      return;
    }
    const resultado = await AtlasApi.pedir(
      "DELETE",
      "/clientes/" + encodeURIComponent(id) + "?confirmar=true"
    );
    idPorEliminar = null;
    if (resultado.ok) {
      AtlasApi.mostrarMensaje(mensaje, "Cliente eliminado correctamente.", "exito");
      cargar();
    } else {
      AtlasApi.mostrarMensaje(mensaje, AtlasApi.mensajeError(resultado), "error");
    }
  });

  cargar();
})();
