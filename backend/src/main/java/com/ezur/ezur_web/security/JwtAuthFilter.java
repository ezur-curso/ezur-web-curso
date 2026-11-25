package com.ezur.ezur_web.security;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ezur.ezur_web.model.EstadoUsuario;
import com.ezur.ezur_web.model.Perfil;
import com.ezur.ezur_web.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lee el token "Authorization: Bearer ..." y deja la identidad en el contexto de seguridad.
 * Un administrador que aún no pasó el 2FA solo recibe ROLE_PRE_2FA: no llega a /api/admin.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UsuarioRepository usuarios;

    public JwtAuthFilter(JwtService jwt, UsuarioRepository usuarios) {
        this.jwt = jwt;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith("Bearer ")) {
            Optional<JwtService.Sesion> sesion = jwt.validar(cabecera.substring(7));
            sesion.flatMap(s -> usuarios.findByUsuario(s.usuario())
                            .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                            .map(u -> new UsernamePasswordAuthenticationToken(
                                    u.getUsuario(), null, autoridades(u.getPerfil(), s.segundoFactor()))))
                    .ifPresent(auth -> SecurityContextHolder.getContext().setAuthentication(auth));
        }
        chain.doFilter(request, response);
    }

    private static List<SimpleGrantedAuthority> autoridades(Perfil perfil, boolean segundoFactor) {
        if (perfil == Perfil.CLIENTE) {
            return List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"));
        }
        return List.of(new SimpleGrantedAuthority(segundoFactor ? "ROLE_ADMIN" : "ROLE_PRE_2FA"));
    }
}
