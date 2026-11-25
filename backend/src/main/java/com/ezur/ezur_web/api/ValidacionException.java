package com.ezur.ezur_web.api;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.validation.BindingResult;

/** Errores de validación por campo; se responden como 400 {"errores": {campo: mensaje}}. */
public class ValidacionException extends RuntimeException {

    private final transient Map<String, String> errores;

    public ValidacionException(Map<String, String> errores) {
        super("Datos inválidos");
        this.errores = errores;
    }

    public ValidacionException(String campo, String mensaje) {
        this(Map.of(campo, mensaje));
    }

    /** Lanza la excepción si el resultado trae errores (para las validaciones que hacen los servicios). */
    public static void siHayErrores(BindingResult result) {
        if (result.hasErrors()) {
            Map<String, String> errores = new LinkedHashMap<>();
            result.getFieldErrors().forEach(e -> errores.putIfAbsent(e.getField(), e.getDefaultMessage()));
            throw new ValidacionException(errores);
        }
    }

    public Map<String, String> getErrores() {
        return errores;
    }
}
