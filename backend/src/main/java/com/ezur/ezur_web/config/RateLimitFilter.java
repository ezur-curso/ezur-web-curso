package com.ezur.ezur_web.config;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;

public class RateLimitFilter extends OncePerRequestFilter {


    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    
    private static final int MAX_LOGIN = 5;
    private static final int MAX_FORMULARIOS = 3;
    private static final long VENTANA_MS = 10 * 60 * 1000;

    private final Map<String, Registro> libreta = new ConcurrentHashMap<>();

    private static class Registro {
        int intentos;
        long inicioVentana;
        Registro(long ahora){
            this.intentos = 0;
            this.inicioVentana = ahora;
        }
    }

    @Override 
    protected boolean shouldNotFilter(HttpServletRequest request){
        String ruta = request.getRequestURI();
        return !("POST".equalsIgnoreCase(request.getMethod()) && RUTAS_VIGILADAS.contains(ruta));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,FilterChain chain) throws ServletException, IOException {

        String ruta = request.getRequestURI();
        String ip = ipDelCliente(request);
        String clave = ip + "|" + ruta;
        int limite = limiteParaRuta(ruta);
        long ahora =  System.currentTimeMillis();

        limpiarVencidos(ahora);

        Registro registro = libreta.compute(clave, (k,r)->{
            if (r == null || ahora - r.inicioVentana > VENTANA_MS ){
                r = new Registro(ahora);
            }
            r.intentos++;
            return  r;
        });

        if (registro.intentos == limite + 1) { 
            log.warn("Rate limit alcanzado: ip={} ruta={}", ip, ruta);
        }

         if (registro.intentos > limite) {
            response.setStatus(429);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Demasiados intentos. Esperá unos minutos y volvé a probar.");
            return;
        }

        chain.doFilter(request, response); 
    }

        private void limpiarVencidos(long ahora) {
        if (libreta.size() > 10_000) {
            libreta.entrySet().removeIf(e -> ahora - e.getValue().inicioVentana > VENTANA_MS);
        }
    }

    private static final Set<String> RUTAS_VIGILADAS = Set.of(
        "/api/auth/login", "/api/auth/registro", "/api/presupuestos", "/api/contacto", "/api/auth/2fa",
        "/api/auth/olvide", "/api/auth/restablecer");

    private int limiteParaRuta(String ruta) {
    return switch (ruta) {
        case "/api/auth/login", "/api/auth/2fa", "/api/auth/restablecer" -> MAX_LOGIN;
        default -> MAX_FORMULARIOS;
        };
    }

    // Detrás del frontend la conexión llega desde su contenedor: la IP real viaja en X-Forwarded-For
    private static String ipDelCliente(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        if (reenviada != null && !reenviada.isBlank()) {
            return reenviada.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
