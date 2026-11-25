package com.ezur.ezur_front.api;

import java.util.Map;

/** Respuesta de error de la API: el estado HTTP, un código opcional y los errores por campo. */
public class ApiException extends RuntimeException {

    private final int estado;
    private final String codigo;
    private final transient Map<String, String> errores;

    public ApiException(int estado, String mensaje, String codigo, Map<String, String> errores) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
        this.errores = errores == null ? Map.of() : errores;
    }

    public int getEstado() {
        return estado;
    }

    public String getCodigo() {
        return codigo;
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
