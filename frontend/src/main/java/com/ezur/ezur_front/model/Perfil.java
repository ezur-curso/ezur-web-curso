package com.ezur.ezur_front.model;

public enum Perfil {
    CLIENTE("Cliente"),
    ADMINISTRADOR("Administrador");

    private final String etiqueta;

    Perfil(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
