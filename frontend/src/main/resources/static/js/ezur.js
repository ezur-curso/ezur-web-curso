/* Ezur · movimiento e interacciones. Todo degrada bien sin JavaScript. */
(function () {
  "use strict";

  var reducido = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  var hayObservador = "IntersectionObserver" in window;

  function todos(selector, raiz) {
    return Array.prototype.slice.call((raiz || document).querySelectorAll(selector));
  }

  /* ---------- Sombra de la barra y paralaje ---------- */
  var barra = document.querySelector(".barra");
  var paralajes = todos("[data-parallax]");
  var pendiente = false;

  function alDesplazar() {
    if (barra) barra.classList.toggle("con-sombra", window.scrollY > 8);
    if (reducido) return;
    var alto = window.innerHeight;
    paralajes.forEach(function (el) {
      var caja = el.parentElement.getBoundingClientRect();
      if (caja.bottom < 0 || caja.top > alto) return;
      var desplazamiento = (caja.top + caja.height / 2 - alto / 2) * -0.1;
      el.style.transform = "translate3d(0," + desplazamiento.toFixed(1) + "px,0) scale(1.15)";
    });
  }
  window.addEventListener(
    "scroll",
    function () {
      if (pendiente) return;
      pendiente = true;
      window.requestAnimationFrame(function () {
        pendiente = false;
        alDesplazar();
      });
    },
    { passive: true }
  );
  alDesplazar();

  /* ---------- Contadores ---------- */
  function contar(el) {
    var meta = parseFloat(el.getAttribute("data-contar"));
    if (isNaN(meta)) return;
    var sufijo = el.getAttribute("data-sufijo") || "";
    var final = meta.toLocaleString("es-ES") + sufijo;
    if (reducido) {
      el.textContent = final;
      return;
    }
    var inicio = null;
    var duracion = 1800;
    function paso(t) {
      if (inicio === null) inicio = t;
      var p = Math.min((t - inicio) / duracion, 1);
      var suave = p === 1 ? 1 : 1 - Math.pow(2, -10 * p);
      el.textContent = Math.round(meta * suave).toLocaleString("es-ES") + sufijo;
      if (p < 1) window.requestAnimationFrame(paso);
      else el.textContent = final;
    }
    window.requestAnimationFrame(paso);
  }

  /* ---------- Aparición al hacer scroll ---------- */
  var revelables = todos("[data-revelar], .pasos, .panel");
  var contadores = todos("[data-contar]");

  function mostrar(el) {
    el.classList.add("visto");
  }

  if (!hayObservador || reducido) {
    revelables.forEach(mostrar);
    contadores.forEach(contar);
  } else {
    var observador = new IntersectionObserver(
      function (entradas) {
        entradas.forEach(function (e) {
          if (!e.isIntersecting) return;
          mostrar(e.target);
          observador.unobserve(e.target);
        });
      },
      { threshold: 0.15, rootMargin: "0px 0px -6% 0px" }
    );
    revelables.forEach(function (el) {
      observador.observe(el);
    });

    var observadorContadores = new IntersectionObserver(
      function (entradas) {
        entradas.forEach(function (e) {
          if (!e.isIntersecting) return;
          contar(e.target);
          observadorContadores.unobserve(e.target);
        });
      },
      { threshold: 0.6 }
    );
    contadores.forEach(function (el) {
      observadorContadores.observe(el);
    });
  }

  /* ---------- Razones: se enciende la fila que pasa por el centro ---------- */
  var razones = todos(".razones li");
  if (razones.length && hayObservador) {
    var observadorRazones = new IntersectionObserver(
      function (entradas) {
        entradas.forEach(function (e) {
          e.target.classList.toggle("activa", e.isIntersecting);
        });
      },
      { rootMargin: "-42% 0px -42% 0px" }
    );
    razones.forEach(function (li) {
      observadorRazones.observe(li);
    });
  } else {
    razones.forEach(function (li) {
      li.classList.add("activa");
    });
  }

  /* ---------- Escenario de servicios con pestañas ---------- */
  var escenario = document.querySelector("[data-escenario]");
  if (escenario) {
    var figuras = todos("figure", escenario);
    var pestanas = todos("[data-pestana]");
    var enlace = document.querySelector("[data-escenario-enlace]");
    var texto = document.querySelector("[data-escenario-texto]");
    var actual = 0;
    var temporizador = null;
    var automatico = !reducido;

    function ir(i) {
      actual = (i + figuras.length) % figuras.length;
      figuras.forEach(function (f, n) {
        f.classList.toggle("activo", n === actual);
      });
      pestanas.forEach(function (p, n) {
        var activa = n === actual;
        p.setAttribute("aria-selected", activa ? "true" : "false");
        if (activa) {
          // Reinicia la barra de progreso
          p.style.animation = "none";
          void p.offsetWidth;
          p.style.animation = "";
        }
      });
      if (enlace) enlace.setAttribute("href", pestanas[actual].getAttribute("data-destino"));
      if (texto) texto.textContent = pestanas[actual].getAttribute("data-resumen");
      programar();
    }

    function programar() {
      window.clearTimeout(temporizador);
      if (!automatico) return;
      temporizador = window.setTimeout(function () {
        ir(actual + 1);
      }, 5500);
    }

    pestanas.forEach(function (p, n) {
      p.addEventListener("click", function () {
        automatico = false;
        escenario.parentElement.classList.add("escenario-detenido");
        ir(n);
      });
    });
    escenario.parentElement.addEventListener("mouseenter", function () {
      window.clearTimeout(temporizador);
      escenario.parentElement.classList.add("escenario-pausa");
    });
    escenario.parentElement.addEventListener("mouseleave", function () {
      escenario.parentElement.classList.remove("escenario-pausa");
      programar();
    });
    ir(0);
  }

  /* ---------- Opiniones en carrusel ---------- */
  var carrusel = document.querySelector("[data-opiniones]");
  if (carrusel) {
    var tarjetas = todos("figure", carrusel);
    var contenedorPuntos = document.querySelector("[data-puntos]");
    var actualOp = 0;
    var tempOp = null;

    if (tarjetas.length > 1 && contenedorPuntos) {
      tarjetas.forEach(function (_, n) {
        var b = document.createElement("button");
        b.type = "button";
        b.setAttribute("aria-label", "Ver opinión " + (n + 1));
        b.addEventListener("click", function () {
          mostrarOp(n);
          window.clearTimeout(tempOp);
        });
        contenedorPuntos.appendChild(b);
      });
    }

    function mostrarOp(i) {
      actualOp = (i + tarjetas.length) % tarjetas.length;
      tarjetas.forEach(function (t, n) {
        t.classList.toggle("activa", n === actualOp);
      });
      todos("button", contenedorPuntos || document.createElement("div")).forEach(function (b, n) {
        b.setAttribute("aria-current", n === actualOp ? "true" : "false");
      });
      if (!reducido && tarjetas.length > 1) {
        window.clearTimeout(tempOp);
        tempOp = window.setTimeout(function () {
          mostrarOp(actualOp + 1);
        }, 7000);
      }
    }
    carrusel.addEventListener("mouseenter", function () {
      window.clearTimeout(tempOp);
    });
    carrusel.addEventListener("mouseleave", function () {
      if (!reducido && tarjetas.length > 1) {
        tempOp = window.setTimeout(function () {
          mostrarOp(actualOp + 1);
        }, 4000);
      }
    });
    mostrarOp(0);
  }

  /* ---------- "Abierto ahora" según la hora de Madrid ---------- */
  var indicador = document.querySelector("[data-abierto]");
  if (indicador) {
    try {
      var horario = { 1: [8, 21], 2: [8, 21], 3: [8, 21], 4: [8, 21], 5: [8, 21], 6: [10, 20], 0: [10, 17] };
      var partes = new Intl.DateTimeFormat("en-GB", {
        timeZone: "Europe/Madrid",
        weekday: "short",
        hour: "numeric",
        minute: "numeric",
        hour12: false,
      }).formatToParts(new Date());
      var dato = {};
      partes.forEach(function (p) {
        dato[p.type] = p.value;
      });
      var dia = { Sun: 0, Mon: 1, Tue: 2, Wed: 3, Thu: 4, Fri: 5, Sat: 6 }[dato.weekday];
      var hora = parseInt(dato.hour, 10) + parseInt(dato.minute, 10) / 60;
      var tramo = horario[dia];
      var abierto = tramo && hora >= tramo[0] && hora < tramo[1];
      indicador.style.color = abierto ? "var(--exito)" : "var(--gris)";
      indicador.innerHTML =
        '<span class="punto-abierto" aria-hidden="true"></span> ' + (abierto ? "Abiertos ahora" : "Cerrado ahora");
      indicador.hidden = false;
    } catch (e) {
      /* sin indicador */
    }
  }

  /* ---------- Avance del formulario ---------- */
  var formulario = document.querySelector("form[data-avance]");
  var relleno = document.querySelector("[data-avance-barra]");
  if (formulario && relleno) {
    var requeridos = todos("[required]", formulario);
    var medidor = relleno.parentElement;
    function actualizar() {
      if (!requeridos.length) return;
      var listos = requeridos.filter(function (c) {
        return c.type === "checkbox" ? c.checked : String(c.value).trim() !== "";
      }).length;
      var porcentaje = Math.round((listos / requeridos.length) * 100);
      relleno.style.width = porcentaje + "%";
      medidor.setAttribute("aria-valuenow", String(porcentaje));
    }
    formulario.addEventListener("input", actualizar);
    formulario.addEventListener("change", actualizar);
    actualizar();
  }

  /* =========================================================
     Escenas ligadas al scroll: camión de la portada, recorrido
     de la mudanza y furgoneta del pie. Un solo bucle de dibujo.
     ========================================================= */
  function limitar(v) {
    return v < 0 ? 0 : v > 1 ? 1 : v;
  }
  function salida(t) {
    return 1 - Math.pow(1 - t, 3);
  }
  function entradaSalida(t) {
    return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
  }
  var escenas = [];

  /* ---------- Portada: las puertas se abren y entramos al camión ---------- */
  var portada = document.querySelector("[data-portada]");
  if (portada && !reducido) {
    portada.classList.add("portada-viva");
    var escenario = portada.querySelector(".portada-escenario");
    var camion = portada.querySelector("[data-camion]");
    var hueco = portada.querySelector(".camion-hueco");
    var puertaIzq = portada.querySelector(".puerta-izq");
    var puertaDer = portada.querySelector(".puerta-der");
    var luz = portada.querySelector(".camion-luz");
    var titulo = portada.querySelector(".portada-titulo");
    var revelado = portada.querySelector(".portada-revelado");
    var velo = portada.querySelector(".portada-velo");
    var pista = portada.querySelector(".portada-pista");
    var entreabierta = 0;
    var geo = null;

    // Medidas sin transformar: centro del hueco y escala para llenar el escenario
    function medir() {
      camion.style.transform = "none";
      var e = escenario.getBoundingClientRect();
      var c = camion.getBoundingClientRect();
      var h = hueco.getBoundingClientRect();
      var cx = h.left + h.width / 2;
      var cy = h.top + h.height / 2;
      camion.style.transformOrigin = (cx - c.left).toFixed(1) + "px " + (cy - c.top).toFixed(1) + "px";
      geo = {
        tx: e.left + e.width / 2 - cx,
        ty: e.top + e.height / 2 - cy,
        k: Math.max(e.width / h.width, e.height / h.height) * 1.12 - 1,
      };
    }

    function dibujarPortada() {
      if (!geo) medir();
      var caja = portada.getBoundingClientRect();
      if (caja.bottom < 0 || caja.top > window.innerHeight) return;
      var recorrido = portada.offsetHeight - escenario.offsetHeight;
      var p = limitar(-caja.top / recorrido);

      var abre = entradaSalida(limitar(p / 0.42));
      var angulo = Math.max(entreabierta, abre * 110);
      puertaIzq.style.transform = "rotateY(" + (-angulo).toFixed(2) + "deg)";
      puertaDer.style.transform = "rotateY(" + angulo.toFixed(2) + "deg)";
      luz.style.opacity = Math.max(abre, entreabierta / 40).toFixed(3);

      var t = limitar((p - 0.1) / 0.24);
      titulo.style.opacity = (1 - t).toFixed(3);
      titulo.style.transform = "translateY(" + (-t * 36).toFixed(1) + "px)";
      pista.style.opacity = (1 - limitar(p / 0.05)).toFixed(3);

      var z = entradaSalida(limitar((p - 0.36) / 0.42));
      camion.style.transform =
        "translate(" + (geo.tx * z).toFixed(1) + "px," + (geo.ty * z).toFixed(1) + "px) scale(" + (1 + geo.k * z).toFixed(4) + ")";

      velo.style.opacity = limitar((p - 0.6) / 0.18).toFixed(3);
      var r = limitar((p - 0.7) / 0.16);
      revelado.style.opacity = r.toFixed(3);
      revelado.style.transform = "translateY(" + ((1 - r) * 22).toFixed(1) + "px)";
      revelado.style.visibility = r > 0.01 ? "visible" : "hidden";
    }

    // Al cargar, las puertas se entreabren un poco: invitan a seguir
    var inicioAjar = null;
    function entreabrir(ts) {
      if (inicioAjar === null) inicioAjar = ts;
      var q = limitar((ts - inicioAjar - 450) / 1100);
      entreabierta = salida(q) * 24;
      dibujarPortada();
      if (q < 1) window.requestAnimationFrame(entreabrir);
    }
    window.requestAnimationFrame(entreabrir);

    window.addEventListener("resize", function () {
      geo = null;
      dibujarPortada();
    });
    escenas.push(dibujarPortada);
  }

  /* ---------- Recorrido: la furgoneta avanza y enciende cada paso ---------- */
  var recorridoEl = document.querySelector("[data-recorrido]");
  if (recorridoEl) {
    var via = recorridoEl.querySelector("[data-via]");
    var trazo = recorridoEl.querySelector("[data-trazo]");
    var furgoneta = recorridoEl.querySelector("[data-furgo]");
    var paradas = todos("[data-parada]", recorridoEl);
    var pasos = todos("[data-paso]", recorridoEl);
    var contadorPaso = recorridoEl.querySelector("[data-paso-actual]");
    var escenarioRec = recorridoEl.querySelector(".recorrido-escenario");
    var largo = via.getTotalLength();
    var objetivos = [150, 450, 750, 1050];

    function largoParaX(x) {
      var bajo = 0;
      var alto = largo;
      for (var n = 0; n < 28; n++) {
        var medio = (bajo + alto) / 2;
        if (via.getPointAtLength(medio).x < x) bajo = medio;
        else alto = medio;
      }
      return bajo;
    }
    var largosParada = objetivos.map(largoParaX);
    paradas.forEach(function (c, n) {
      var pt = via.getPointAtLength(largosParada[n]);
      c.setAttribute("cx", pt.x.toFixed(1));
      c.setAttribute("cy", pt.y.toFixed(1));
    });
    trazo.style.strokeDasharray = largo.toFixed(1);

    function pintarRecorrido(avance) {
      var l = avance * largo;
      trazo.style.strokeDashoffset = (largo - l).toFixed(1);
      var a = via.getPointAtLength(Math.max(l - 1, 0));
      var b = via.getPointAtLength(Math.min(l + 1, largo));
      var pt = via.getPointAtLength(l);
      var giro = (Math.atan2(b.y - a.y, b.x - a.x) * 180) / Math.PI;
      furgoneta.setAttribute("transform", "translate(" + pt.x.toFixed(1) + " " + pt.y.toFixed(1) + ") rotate(" + giro.toFixed(1) + ")");
      var activos = 0;
      largosParada.forEach(function (lp, n) {
        var encendido = l >= lp - 4;
        if (encendido) activos = n + 1;
        paradas[n].classList.toggle("activa", encendido);
        pasos[n].classList.toggle("activo", encendido);
      });
      if (contadorPaso) contadorPaso.textContent = String(Math.max(activos, 1));
    }

    var escritorio = window.matchMedia("(min-width: 64rem)");
    function modoRecorrido() {
      var vivo = !reducido && escritorio.matches;
      recorridoEl.classList.toggle("recorrido-vivo", vivo);
      if (!vivo) pintarRecorrido(1);
      else dibujarRecorrido();
    }
    function dibujarRecorrido() {
      if (!recorridoEl.classList.contains("recorrido-vivo")) return;
      var caja = recorridoEl.getBoundingClientRect();
      if (caja.bottom < 0 || caja.top > window.innerHeight) return;
      var distancia = recorridoEl.offsetHeight - escenarioRec.offsetHeight;
      var p = limitar(-caja.top / distancia);
      pintarRecorrido(limitar((p - 0.05) / 0.82));
    }
    if (escritorio.addEventListener) escritorio.addEventListener("change", modoRecorrido);
    modoRecorrido();
    escenas.push(dibujarRecorrido);
  }

  /* ---------- Pie: la furgoneta cruza mientras aparece ---------- */
  var pie = document.querySelector("[data-pie]");
  var furgoPie = document.querySelector("[data-pie-furgo]");
  if (pie && furgoPie) {
    var rutaPie = furgoPie.parentElement;
    function dibujarPie() {
      var caja = pie.getBoundingClientRect();
      var alto = window.innerHeight;
      if (caja.top > alto) return;
      var q = reducido ? 0.5 : limitar((alto - caja.top) / (alto * 0.95));
      var recorridoPie = rutaPie.clientWidth - furgoPie.getBoundingClientRect().width;
      furgoPie.style.transform = "translateX(" + (q * recorridoPie).toFixed(1) + "px)";
    }
    escenas.push(dibujarPie);
  }

  if (escenas.length) {
    var pendienteEscena = false;
    function dibujarEscenas() {
      pendienteEscena = false;
      escenas.forEach(function (f) {
        f();
      });
    }
    window.addEventListener(
      "scroll",
      function () {
        if (pendienteEscena) return;
        pendienteEscena = true;
        window.requestAnimationFrame(dibujarEscenas);
      },
      { passive: true }
    );
    window.addEventListener("resize", dibujarEscenas);
    dibujarEscenas();
  }

  /* =========================================================
     Secciones de la tercera versión
     ========================================================= */
  function acotar(v) {
    return v < 0 ? 0 : v > 1 ? 1 : v;
  }
  var dibujos = [];

  /* ---------- Calculadora: el camión se llena según el tamaño ---------- */
  var calc = document.querySelector("[data-calculadora]");
  if (calc) {
    var opciones = todos(".calc-opcion", calc);
    var cajasSvg = todos("[data-cajas-grupo] rect", calc);
    var salidas = {};
    todos("[data-salida]", calc).forEach(function (el) {
      salidas[el.getAttribute("data-salida")] = el;
    });
    var campoTipo = calc.querySelector('[data-campo="tipo"]');
    var campoDetalle = calc.querySelector('[data-campo="detalle"]');
    var maxM3 = 35;

    function animarNumero(el, hasta) {
      var desde = parseInt(el.textContent, 10) || 0;
      if (reducido || desde === hasta) {
        el.textContent = String(hasta);
        return;
      }
      var inicio = null;
      function paso(t) {
        if (inicio === null) inicio = t;
        var p = Math.min((t - inicio) / 450, 1);
        var suave = 1 - Math.pow(1 - p, 3);
        el.textContent = String(Math.round(desde + (hasta - desde) * suave));
        if (p < 1) window.requestAnimationFrame(paso);
      }
      window.requestAnimationFrame(paso);
    }

    function elegir(boton) {
      opciones.forEach(function (b) {
        b.setAttribute("aria-checked", b === boton ? "true" : "false");
        b.tabIndex = b === boton ? 0 : -1;
      });
      var m3 = parseInt(boton.getAttribute("data-m3"), 10);
      var visibles = Math.round((m3 / maxM3) * cajasSvg.length);
      cajasSvg.forEach(function (r, n) {
        r.classList.toggle("fuera", n >= visibles);
        r.style.transitionDelay = (reducido ? 0 : Math.abs(n - visibles) * 18) + "ms";
      });
      animarNumero(salidas.m3, m3);
      animarNumero(salidas.equipo, parseInt(boton.getAttribute("data-equipo"), 10));
      animarNumero(salidas.cajas, parseInt(boton.getAttribute("data-cajas"), 10));
      salidas.horas.textContent = boton.getAttribute("data-horas");
      campoTipo.value = boton.getAttribute("data-tipo");
      campoDetalle.value =
        boton.getAttribute("data-nombre") + ". Estimación de la web: camión de " + m3 + " m³, " +
        boton.getAttribute("data-equipo") + " personas, unas " + boton.getAttribute("data-cajas") + " cajas.";
    }

    opciones.forEach(function (b, n) {
      b.addEventListener("click", function () {
        elegir(b);
      });
      // Flechas del teclado, como en un grupo de radios
      b.addEventListener("keydown", function (e) {
        var dir = e.key === "ArrowRight" || e.key === "ArrowDown" ? 1 : e.key === "ArrowLeft" || e.key === "ArrowUp" ? -1 : 0;
        if (!dir) return;
        e.preventDefault();
        var siguiente = opciones[(n + dir + opciones.length) % opciones.length];
        siguiente.focus();
        elegir(siguiente);
      });
    });
    elegir(calc.querySelector('.calc-opcion[aria-checked="true"]') || opciones[0]);
  }

  /* ---------- Índice de servicios: foto que sigue al cursor ---------- */
  var indice = document.querySelector("[data-indice]");
  var flotante = document.querySelector("[data-flotante]");
  if (indice && flotante && window.matchMedia("(hover: hover) and (pointer: fine)").matches && !reducido) {
    var imgFlotante = flotante.querySelector("img");
    var cx = 0, cy = 0, fx = 0, fy = 0, siguiendo = false;
    function mover() {
      fx += (cx - fx) * 0.18;
      fy += (cy - fy) * 0.18;
      flotante.style.transform = "translate3d(" + (fx + 24).toFixed(1) + "px," + (fy - 110).toFixed(1) + "px,0)";
      if (siguiendo) window.requestAnimationFrame(mover);
    }
    todos(".indice a", indice).forEach(function (a) {
      a.addEventListener("mouseenter", function (e) {
        var ruta = a.querySelector("img").getAttribute("src");
        if (imgFlotante.getAttribute("src") !== ruta) imgFlotante.setAttribute("src", ruta);
        if (!siguiendo) {
          fx = cx = e.clientX;
          fy = cy = e.clientY;
          siguiendo = true;
          window.requestAnimationFrame(mover);
        }
        flotante.classList.add("visible");
      });
      a.addEventListener("mouseleave", function () {
        flotante.classList.remove("visible");
      });
    });
    indice.addEventListener("mousemove", function (e) {
      cx = e.clientX;
      cy = e.clientY;
    });
    indice.addEventListener("mouseleave", function () {
      siguiendo = false;
      flotante.classList.remove("visible");
    });
  }

  /* ---------- Chat de preguntas ---------- */
  var chat = document.querySelector("[data-chat]");
  if (chat) {
    var hilo = chat.querySelector("[data-hilo]");
    var ocupado = false;
    function burbuja(texto, clase) {
      var p = document.createElement("p");
      p.className = "burbuja " + clase;
      p.textContent = texto;
      hilo.appendChild(p);
      hilo.scrollTop = hilo.scrollHeight;
      return p;
    }
    todos("[data-opciones] button", chat).forEach(function (b) {
      b.addEventListener("click", function () {
        if (ocupado || b.disabled) return;
        ocupado = true;
        b.disabled = true;
        burbuja(b.textContent, "burbuja-cliente");
        var espera = document.createElement("p");
        espera.className = "burbuja burbuja-ezur";
        espera.setAttribute("aria-label", "Escribiendo");
        espera.innerHTML = '<span class="escribiendo"><i></i><i></i><i></i></span>';
        hilo.appendChild(espera);
        hilo.scrollTop = hilo.scrollHeight;
        window.setTimeout(function () {
          espera.remove();
          burbuja(b.getAttribute("data-respuesta"), "burbuja-ezur");
          ocupado = false;
        }, reducido ? 0 : 850);
      });
    });
  }

  /* ---------- Servicios: el visor sigue al capítulo que se lee ---------- */
  var relato = document.querySelector("[data-relato]");
  if (relato && hayObservador) {
    var fotosVisor = todos(".visor-marco img", relato);
    var marcas = todos(".visor-progreso span", relato);
    var observadorRelato = new IntersectionObserver(
      function (entradas) {
        entradas.forEach(function (e) {
          if (!e.isIntersecting) return;
          var n = parseInt(e.target.getAttribute("data-capitulo"), 10);
          fotosVisor.forEach(function (img, i) {
            img.classList.toggle("activa", i === n);
          });
          marcas.forEach(function (m, i) {
            m.classList.toggle("activo", i <= n);
          });
        });
      },
      { rootMargin: "-45% 0px -45% 0px" }
    );
    todos("[data-capitulo]", relato).forEach(function (c) {
      observadorRelato.observe(c);
    });
  }

  /* ---------- Nosotros: palabra que cambia ---------- */
  var rotador = document.querySelector("[data-rotador]");
  if (rotador) {
    var palabras = todos("span", rotador);
    var indicePalabra = 0;
    palabras[0].classList.add("actual");
    if (!reducido && palabras.length > 1) {
      window.setInterval(function () {
        var actual = palabras[indicePalabra];
        indicePalabra = (indicePalabra + 1) % palabras.length;
        var siguiente = palabras[indicePalabra];
        actual.classList.remove("actual");
        actual.classList.add("sale");
        siguiente.classList.remove("sale");
        siguiente.classList.add("actual");
        window.setTimeout(function () {
          actual.classList.remove("sale");
        }, 750);
      }, 2200);
    }
  }

  /* ---------- Nosotros: la historia avanza en horizontal ---------- */
  var historia = document.querySelector("[data-historia]");
  if (historia) {
    var pista = historia.querySelector("[data-pista]");
    var escenarioHist = historia.querySelector(".historia-escenario");
    var anchoGrande = window.matchMedia("(min-width: 64rem)");
    var sobrante = 0;
    function prepararHistoria() {
      var viva = !reducido && anchoGrande.matches;
      historia.classList.toggle("historia-viva", viva);
      if (!viva) {
        historia.style.height = "";
        pista.style.transform = "";
        return;
      }
      sobrante = Math.max(pista.scrollWidth - window.innerWidth + 80, 0);
      historia.style.height = escenarioHist.offsetHeight + sobrante + "px";
      dibujarHistoria();
    }
    function dibujarHistoria() {
      if (!historia.classList.contains("historia-viva")) return;
      var caja = historia.getBoundingClientRect();
      if (caja.bottom < 0 || caja.top > window.innerHeight) return;
      var p = acotar(-caja.top / (historia.offsetHeight - escenarioHist.offsetHeight || 1));
      pista.style.transform = "translate3d(" + (-p * sobrante).toFixed(1) + "px,0,0)";
    }
    if (anchoGrande.addEventListener) anchoGrande.addEventListener("change", prepararHistoria);
    window.addEventListener("resize", prepararHistoria);
    window.addEventListener("load", prepararHistoria);
    prepararHistoria();
    dibujos.push(dibujarHistoria);
  }

  /* ---------- Nosotros: la jornada avanza con el scroll ---------- */
  var jornada = document.querySelector("[data-jornada] .jornada");
  if (jornada) {
    var pasosJornada = todos(".jornada-paso", jornada);
    if (reducido) {
      pasosJornada.forEach(function (p) {
        p.classList.add("hecho");
      });
    } else {
      jornada.parentElement.parentElement.classList.add("jornada-viva");
      var dibujarJornada = function () {
        var caja = jornada.getBoundingClientRect();
        var linea = window.innerHeight * 0.6;
        jornada.style.setProperty("--avance", acotar((linea - caja.top) / caja.height).toFixed(3));
        pasosJornada.forEach(function (p) {
          p.classList.toggle("hecho", p.getBoundingClientRect().top < linea);
        });
      };
      dibujos.push(dibujarJornada);
    }
  }

  if (dibujos.length) {
    var pendienteDibujo = false;
    window.addEventListener(
      "scroll",
      function () {
        if (pendienteDibujo) return;
        pendienteDibujo = true;
        window.requestAnimationFrame(function () {
          pendienteDibujo = false;
          dibujos.forEach(function (f) {
            f();
          });
        });
      },
      { passive: true }
    );
    dibujos.forEach(function (f) {
      f();
    });
  }
})();
