package com.ezur.ezur_front.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CambioClaveRequest {

    @NotBlank(message = "Escribe tu clave actual")
    private String actual;

    @NotBlank(message = "Escribe la clave nueva")
    @Size(min = 10, max = 72, message = "La clave nueva debe tener entre 10 y 72 caracteres")
    private String nueva;

    @NotBlank(message = "Repite la clave nueva")
    private String confirmacion;

    public String getActual() { return actual; }
    public void setActual(String actual) { this.actual = actual; }

    public String getNueva() { return nueva; }
    public void setNueva(String nueva) { this.nueva = nueva; }

    public String getConfirmacion() { return confirmacion; }
    public void setConfirmacion(String confirmacion) { this.confirmacion = confirmacion; }
}
