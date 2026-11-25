package com.ezur.ezur_front.security;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Quién tiene la sesión abierta en el frontend. Guarda el token de la API: vive en la sesión del
 * servidor y nunca llega al navegador.
 */
public class UsuarioApi implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final String usuario;
    private final List<SimpleGrantedAuthority> autoridades;
    private volatile String token;

    public UsuarioApi(String usuario, String perfil, String token) {
        this.usuario = usuario;
        this.autoridades = List.of(new SimpleGrantedAuthority(
                "ADMINISTRADOR".equals(perfil) ? "ROLE_ADMIN" : "ROLE_CLIENTE"));
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    /** Tras el 2FA la API entrega un token nuevo, con permiso de administración. */
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return autoridades;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return usuario;
    }
}
