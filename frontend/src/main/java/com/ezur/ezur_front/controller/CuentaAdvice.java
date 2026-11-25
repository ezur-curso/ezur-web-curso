package com.ezur.ezur_front.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Datos de la sesión para el menú público: "Iniciar sesión" o "Mi cuenta". */
@ControllerAdvice
public class CuentaAdvice {

    @Value("${empresa.nombre}")
    private String nombre;

    @Value("${empresa.telefono}")
    private String telefono;

    @Value("${empresa.telefono-enlace}")
    private String telefonoEnlace;

    @Value("${empresa.email}")
    private String email;

    @Value("${empresa.direccion}")
    private String direccion;

    /** Datos de contacto de la empresa para el pie, contacto y espacio de cliente. */
    @ModelAttribute("empresa")
    public Map<String, String> empresa() {
        return Map.of("nombre", nombre, "telefono", telefono, "telefonoEnlace", telefonoEnlace,
                "email", email, "direccion", direccion);
    }

    @ModelAttribute("cuentaNombre")
    public String cuentaNombre() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return estaAutenticado(auth) ? auth.getName() : null;
    }

    @ModelAttribute("cuentaUrl")
    public String cuentaUrl() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!estaAutenticado(auth)) {
            return null;
        }
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return admin ? "/admin" : "/cliente";
    }

    private boolean estaAutenticado(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }
}
