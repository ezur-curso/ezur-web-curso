package com.ezur.ezur_front.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.ModelAndView;

import com.ezur.ezur_front.api.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/** Qué ve el visitante cuando la API responde con un error que el formulario no sabe corregir. */
@ControllerAdvice
public class ApiErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(ApiErrorAdvice.class);

    @ExceptionHandler(ApiException.class)
    public Object alFallarLaApi(ApiException e, HttpServletRequest request) {
        switch (e.getEstado()) {
            case 401, 403 -> {
                // El token venció o ya no vale: hay que entrar de nuevo
                HttpSession sesion = request.getSession(false);
                if (sesion != null) {
                    sesion.invalidate();
                }
                SecurityContextHolder.clearContext();
                String acceso = request.getRequestURI().startsWith("/admin") || request.getRequestURI().equals("/2fa")
                        ? "/admin-login" : "/login";
                return new ModelAndView("redirect:" + acceso + "?expirada");
            }
            case 404 -> {
                return pagina("error/404", HttpStatus.NOT_FOUND);
            }
            case 429 -> {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                        .body("Demasiados intentos. Espera unos minutos y vuelve a probar.");
            }
            default -> {
                log.error("La API respondió {}: {}", e.getEstado(), e.getMessage());
                return pagina("error/500", HttpStatus.BAD_GATEWAY);
            }
        }
    }

    @ExceptionHandler(RestClientException.class)
    public ModelAndView apiNoDisponible(RestClientException e) {
        log.error("No se pudo contactar con la API", e);
        return pagina("error/500", HttpStatus.SERVICE_UNAVAILABLE);
    }

    private ModelAndView pagina(String vista, HttpStatus estado) {
        ModelAndView mv = new ModelAndView(vista);
        mv.setStatus(estado);
        return mv;
    }
}
