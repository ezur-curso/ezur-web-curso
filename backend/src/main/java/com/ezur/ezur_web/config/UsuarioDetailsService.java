package com.ezur.ezur_web.config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ezur.ezur_web.model.EstadoUsuario;
import com.ezur.ezur_web.model.Perfil;
import com.ezur.ezur_web.model.Usuario;
import com.ezur.ezur_web.repository.UsuarioRepository;

@Service 
public class UsuarioDetailsService implements UserDetailsService{
    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository){
        this.usuarioRepository = usuarioRepository;
    }

    @Override 
    public UserDetails loadUserByUsername(String nombreUsuario) throws UsernameNotFoundException{
        Usuario usuario = usuarioRepository.findByUsuario(nombreUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        String rol = usuario.getPerfil() == Perfil.ADMINISTRADOR ? "ROLE_ADMIN" : "ROLE_CLIENTE";

        // Solo el usuario ACTIVO puede entrar; PENDIENTE y ANULADO quedan deshabilitados
        return User.builder()
                .username(usuario.getUsuario())
                .password(usuario.getPassword())
                .authorities(new SimpleGrantedAuthority(rol))
                .disabled(usuario.getEstado() != EstadoUsuario.ACTIVO)
                .build();
    }
}
