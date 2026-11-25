package com.ezur.ezur_web.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ContactoRequest {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre es demasiado largo")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Correo inválido")
    @Size(max = 150, message = "El correo es demasiado largo")
    private String email;

    @Size(max = 30, message = "El teléfono es demasiado largo")
    private String telefono;

    @Size(max = 150, message = "El asunto es demasiado largo")
    private String asunto;

    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 2000, message = "El mensaje es demasiado largo (máximo 2000 caracteres)")
    private String mensaje;

    @NotNull(message = "Debes aceptar la política de privacidad")
    @AssertTrue(message = "Debes aceptar la política de privacidad")
    private Boolean aceptaPrivacidad;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public Boolean getAceptaPrivacidad() {
        return aceptaPrivacidad;
    }

    public void setAceptaPrivacidad(Boolean aceptaPrivacidad) {
        this.aceptaPrivacidad = aceptaPrivacidad;
    }


}
