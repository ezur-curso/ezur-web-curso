package com.ezur.ezur_front.security;

import java.io.IOException;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/** Un administrador con solo la contraseña no pasa de /2fa: hasta verificar el código no entra a /admin. */
public class SegundoFactorFilter extends OncePerRequestFilter {

    public static final String SESION_2FA_OK = "2FA_OK";
    public static final String SESION_SECRETO_NUEVO = "2FA_SECRETO_NUEVO";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/admin");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean esAdministrador = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Solo el administrador pasa por el código; a un cliente la autorización le responde 403
        if (esAdministrador) {
            HttpSession sesion = request.getSession(false);
            boolean pasoCodigo = sesion != null && auth.getName().equals(sesion.getAttribute(SESION_2FA_OK));

            if (!pasoCodigo) {
                response.sendRedirect("/2fa");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
