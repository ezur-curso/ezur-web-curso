package com.ezur.ezur_web.model;

public enum EstadoUsuario {
    ACTIVO("Activo"),
    PENDIENTE("Pendiente"),
    ANULADO("Anulado");

    private final String etiqueta;

    EstadoUsuario(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
