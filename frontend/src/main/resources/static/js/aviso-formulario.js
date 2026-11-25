document.addEventListener("DOMContentLoaded", function () {
  var aviso = document.querySelector("[data-aviso]");
  if (aviso) {
    aviso.scrollIntoView({ behavior: "smooth", block: "center" });
  }
});