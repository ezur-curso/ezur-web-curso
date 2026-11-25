package com.ezur.ezur_web.api;

import java.time.LocalDateTime;

import com.ezur.ezur_web.model.EstadoUsuario;
import com.ezur.ezur_web.model.Perfil;
import com.ezur.ezur_web.model.Usuario;

/** Lo que la API cuenta de un usuario: nunca la clave ni el secreto del 2FA. */
public record UsuarioDto(Long id, String dni, String nombres, String apellidos, String nombreCompleto,
                         String usuario, String email, Perfil perfil, EstadoUsuario estado,
                         LocalDateTime fechaRegistro) {

    public static UsuarioDto de(Usuario u) {
        return new UsuarioDto(u.getId(), u.getDni(), u.getNombres(), u.getApellidos(), u.getNombreCompleto(),
                u.getUsuario(), u.getEmail(), u.getPerfil(), u.getEstado(), u.getFechaRegistro());
    }
}
