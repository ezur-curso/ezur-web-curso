document.addEventListener("DOMContentLoaded", () => {
  document.querySelectorAll("form").forEach((form) => {
    form.addEventListener("submit", () => {
      const boton = form.querySelector('button[type="submit"]');
      if (!boton) return;
      boton.disabled = true;
      boton.style.opacity = "0.6";
      boton.style.cursor = "not-allowed";
    });
  });
});

window.addEventListener("pageshow", (event) => {
  if (!event.persisted) return;
  document.querySelectorAll('button[type="submit"]').forEach((boton) => {
    boton.disabled = false;
    boton.style.opacity = "";
    boton.style.cursor = "";
  });
});
