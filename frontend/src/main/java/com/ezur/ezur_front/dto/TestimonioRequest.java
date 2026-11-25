package com.ezur.ezur_front.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TestimonioRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre es demasiado largo")
    private String nombre;

    @NotBlank(message = "El texto es obligatorio")
    @Size(max = 500, message = "El texto es demasiado largo (máximo 500 caracteres)")
    private String texto;

    @Size(max = 100, message = "El servicio es demasiado largo")
    private String servicio;

    @NotNull(message = "Elige las estrellas")
    @Min(value = 1, message = "Mínimo 1 estrella")
    @Max(value = 5, message = "Máximo 5 estrellas")
    private Integer estrellas = 5;

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public String getServicio() { return servicio; }
    public void setServicio(String servicio) { this.servicio = servicio; }

    public Integer getEstrellas() { return estrellas; }
    public void setEstrellas(Integer estrellas) { this.estrellas = estrellas; }
}