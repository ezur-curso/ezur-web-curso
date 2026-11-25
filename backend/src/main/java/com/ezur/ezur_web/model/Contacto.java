package com.ezur.ezur_web.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity 

@Table (name = "contactos")
public class Contacto {
    
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private long id;


    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "Correo inválido")
    private String email;

    private String telefono;

    private String asunto;

    @NotBlank(message = "El mensaje es obligatorio")
    @Column(columnDefinition = "TEXT")
    private String mensaje;

    private LocalDateTime fechaCreacion = LocalDateTime.now();

     private Boolean aceptaPrivacidad;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

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

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Boolean getAceptaPrivacidad() { 
        return aceptaPrivacidad; 
    }
    public void setAceptaPrivacidad(Boolean aceptaPrivacidad) {
         this.aceptaPrivacidad = aceptaPrivacidad; 
    }
}
