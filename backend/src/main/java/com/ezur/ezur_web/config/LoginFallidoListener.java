package com.ezur.ezur_web.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
public class LoginFallidoListener {

    private static final Logger log = LoggerFactory.getLogger(LoginFallidoListener.class);

    @EventListener
    public void alFallarElLogin(AuthenticationFailureBadCredentialsEvent evento) {
        String email = limpiar(evento.getAuthentication().getName());

        Object detalles = evento.getAuthentication().getDetails();
        String ip = (detalles instanceof WebAuthenticationDetails web)
                ? web.getRemoteAddress()
                : "desconocida";

        log.warn("Login fallido: email={} ip={}", email, ip);
    }

    private String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinSaltos = texto.replaceAll("[\\r\\n\\t]", "_");
        return sinSaltos.length() > 100 ? sinSaltos.substring(0, 100) : sinSaltos;
    }
}