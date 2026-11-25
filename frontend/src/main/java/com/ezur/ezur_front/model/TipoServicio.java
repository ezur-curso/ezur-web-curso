package com.ezur.ezur_front.model;


public class TipoServicio {

    private Long id;

    private String nombre;

    public TipoServicio() {
    }

    public TipoServicio(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
