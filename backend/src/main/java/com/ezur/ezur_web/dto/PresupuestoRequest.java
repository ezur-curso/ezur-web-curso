package com.ezur.ezur_web.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PresupuestoRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre es demasiado largo")
    private String nombreCompleto;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Correo inválido")
    @Size(max = 150, message = "El correo es demasiado largo")
    private String email;

    @NotBlank(message = "El telefono es obligatorio")
    @Size(max = 30, message = "El teléfono es demasiado largo")
    private String telefono;

    @NotBlank(message = "La direccion del origen es obligatorio")
    @Size(max = 200, message = "La dirección de origen es demasiado larga")
    private String direccionOrigen;

    @NotBlank(message = "La direcccion del destino es obligatorio")
    @Size(max = 200, message = "La dirección de destino es demasiado larga")
    private String direccionDestino;

    @NotBlank(message = "El tipo de mudanza es obligatorio")
    @Size(max = 50, message = "El tipo de mudanza no es válido")
    private String tipoMudanza;

    private LocalDate fechaEstimada;

    @Size(max = 2000, message = "Los comentarios son demasiado largos (máximo 2000 caracteres)")
    private String comentarios;

    @NotNull(message = "Debes aceptar la política de privacidad")
    @AssertTrue(message = "Debes aceptar la política de privacidad")
    private Boolean aceptaPrivacidad;

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
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

    public String getDireccionOrigen() {
        return direccionOrigen;
    }

    public void setDireccionOrigen(String direccionOrigen) {
        this.direccionOrigen = direccionOrigen;
    }

    public String getDireccionDestino() {
        return direccionDestino;
    }

    public void setDireccionDestino(String direccionDestino) {
        this.direccionDestino = direccionDestino;
    }

    public String getTipoMudanza() {
        return tipoMudanza;
    }

    public void setTipoMudanza(String tipoMudanza) {
        this.tipoMudanza = tipoMudanza;
    }

    public LocalDate getFechaEstimada() {
        return fechaEstimada;
    }

    public void setFechaEstimada(LocalDate fechaEstimada) {
        this.fechaEstimada = fechaEstimada;
    }

    public String getComentarios() {
        return comentarios;
    }

    public void setComentarios(String comentarios) {
        this.comentarios = comentarios;
    }

    public Boolean getAceptaPrivacidad() {
        return aceptaPrivacidad;
    }

    public void setAceptaPrivacidad(Boolean aceptaPrivacidad) {
        this.aceptaPrivacidad = aceptaPrivacidad;
    }
    
}

