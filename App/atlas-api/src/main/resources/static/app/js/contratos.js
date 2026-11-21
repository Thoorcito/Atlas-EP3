/* Atlas - contratos de un cliente con su estado calculado (RF-5.1, RF-5.4, RB-3). */
"use strict";

(() => {
  if (!AtlasApi.exigirSesion()) {
    return;
  }
  AtlasApi.iniciarEncabezado();

  const mensaje = document.getElementById("mensaje");
  const cuerpoTabla = document.querySelector("#tabla-contratos tbody");
  const idCliente = new URLSearchParams(window.location.search).get("cliente");

  const ETIQUETAS = { ACTIVO: "ACTIVO", POR_VENCER: "POR VENCER", EXPIRADO: "EXPIRADO" };

  function formatoFecha(iso) {
    // "2026-12-31" -> "31-12-2026" (formato chileno)
    if (typeof iso !== "string" || iso.length !== 10) {
      return iso;
    }
    return iso.substring(8, 10) + "-" + iso.substring(5, 7) + "-" + iso.substring(0, 4);
  }

  async function cargar() {
    document.body.dataset.cargando = "true";
    try {
      await cargarDatos();
    } finally {
      document.body.dataset.cargando = "false";
    }
  }

  async function cargarDatos() {
    if (!/^[0-9]+$/.test(idCliente || "")) {
      AtlasApi.mostrarMensaje(mensaje, "El cliente indicado no es válido.", "error");
      return;
    }
    const cliente = await AtlasApi.pedir("GET", "/clientes/" + idCliente);
    if (!cliente.ok) {
      AtlasApi.mostrarMensaje(mensaje, AtlasApi.mensajeError(cliente), "error");
      return;
    }
    document.getElementById("nombre-cliente").textContent = cliente.datos.nombre;
    document.getElementById("rut-cliente").textContent = cliente.datos.rut;

    const contratos = await AtlasApi.pedir("GET", "/clientes/" + idCliente + "/contratos");
    if (!contratos.ok) {
      AtlasApi.mostrarMensaje(mensaje, AtlasApi.mensajeError(contratos), "error");
      return;
    }
    cuerpoTabla.replaceChildren();
    contratos.datos.forEach((contrato) => {
      const tr = document.createElement("tr");
      tr.dataset.id = String(contrato.id);
      tr.appendChild(AtlasApi.celda(contrato.titulo, "col-titulo"));
      tr.appendChild(AtlasApi.celda(formatoFecha(contrato.fechaInicio)));
      tr.appendChild(AtlasApi.celda(formatoFecha(contrato.fechaTermino)));
      const estado = AtlasApi.celda("", "col-estado");
      const insignia = document.createElement("span");
      insignia.className = "estado estado-" + String(contrato.estado).toLowerCase();
      insignia.textContent = ETIQUETAS[contrato.estado] || contrato.estado;
      estado.appendChild(insignia);
      tr.appendChild(estado);
      cuerpoTabla.appendChild(tr);
    });
    document.getElementById("sin-contratos").hidden = contratos.datos.length > 0;
  }

  cargar();
})();
