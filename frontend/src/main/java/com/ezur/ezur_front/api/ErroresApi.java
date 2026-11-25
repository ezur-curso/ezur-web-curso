package com.ezur.ezur_front.api;

import org.springframework.validation.BindingResult;

/** Pasa los errores por campo que devuelve la API al formulario, como si los hubiera detectado Spring. */
public final class ErroresApi {

    private ErroresApi() {}

    /** @return true si el error era de validación y quedó en el formulario */
    public static boolean aplicar(ApiException e, BindingResult result) {
        if (e.getEstado() != 400 || e.getErrores().isEmpty()) {
            return false;
        }
        e.getErrores().forEach((campo, mensaje) -> {
            if (result.getFieldType(campo) != null) {
                result.rejectValue(campo, "api." + campo, mensaje);
            } else {
                result.reject("api." + campo, mensaje);
            }
        });
        return true;
    }
}
