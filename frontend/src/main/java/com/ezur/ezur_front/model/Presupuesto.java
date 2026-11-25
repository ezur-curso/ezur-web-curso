package com.ezur.ezur_front.model;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Presupuesto {
    
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombreCompleto;

    @NotBlank (message = "El correo es obligatorio")
    private String email;

    @NotBlank (message = "El telefono es obligatorio")
    private String telefono;

    @NotBlank  (message = "La direccion del origen es obligatorio")
    private String direccionOrigen;

    @NotBlank (message = "La direcccion del destino es obligatorio")
    private String direccionDestino;

    @NotBlank  (message = "El tipo de mudanza es obligatorio")
    private String tipoMudanza;

    private LocalDate fechaEstimada;

    private String comentarios;

    private EstadoPresupuesto estado = EstadoPresupuesto.PENDIENTE;

    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
     private Boolean aceptaPrivacidad;

    // Cuenta de cliente que hizo la solicitud estando con sesión iniciada (null si fue anónima)
    private Long usuarioId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public EstadoPresupuesto getEstado() {
        return estado;
    }

    public void setEstado(EstadoPresupuesto estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Boolean getAceptaPrivacidad() { 
        return aceptaPrivacidad; 
    }

    public void setAceptaPrivacidad(Boolean aceptaPrivacidad) { 
        this.aceptaPrivacidad = aceptaPrivacidad; 
    }
    

}
