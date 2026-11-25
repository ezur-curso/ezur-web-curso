package com.ezur.ezur_front.controller;

import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ezur.ezur_front.api.ApiClient;
import com.ezur.ezur_front.api.ApiException;

@Controller
public class PasswordResetController {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);
    private static final int LARGO_MINIMO = 10;
    private static final int BYTES_MAXIMOS = 72; // límite de BCrypt

    private final ApiClient api;

    public PasswordResetController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/olvide-password")
    public String formularioSolicitud() {
        return "olvide-password";
    }

    @PostMapping("/olvide-password")
    public String solicitar(@RequestParam String email) {
        try {
            api.olvidePassword(email.trim());
        } catch (Exception e) {
            // Siempre se responde igual para no revelar qué correos existen
            log.error("No se pudo procesar la solicitud de restablecimiento", e);
        }
        return "redirect:/olvide-password?enviado";
    }

    @GetMapping("/restablecer-password")
    public String formularioReset(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("tokenInvalido", !api.tokenDeRestablecerValido(token));
        model.addAttribute("token", token);
        return "restablecer-password";
    }

    @PostMapping("/restablecer-password")
    public String restablecer(@RequestParam String token,
                              @RequestParam String password,
                              @RequestParam String confirmacion,
                              Model model) {
        model.addAttribute("token", token);

        if (!password.equals(confirmacion)) {
            model.addAttribute("error", "Las contraseñas no coinciden.");
            return "restablecer-password";
        }
        if (password.length() < LARGO_MINIMO) {
            model.addAttribute("error", "La contraseña debe tener al menos " + LARGO_MINIMO + " caracteres.");
            return "restablecer-password";
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS) {
            model.addAttribute("error", "La contraseña es demasiado larga.");
            return "restablecer-password";
        }
        try {
            api.restablecerPassword(token, password);
        } catch (ApiException e) {
            if (e.getEstado() == 410) {
                model.addAttribute("tokenInvalido", true);
                return "restablecer-password";
            }
            if (e.getEstado() == 400 && e.getErrores().containsKey("clave")) {
                model.addAttribute("error", e.getErrores().get("clave"));
                return "restablecer-password";
            }
            throw e;
        }
        return "redirect:/login?reset";
    }
}
