package com.ezur.ezur_web.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.ezur.ezur_web.model.EstadoUsuario;
import com.ezur.ezur_web.model.Perfil;
import com.ezur.ezur_web.model.TipoServicio;
import com.ezur.ezur_web.model.Usuario;
import com.ezur.ezur_web.repository.TipoServicioRepository;
import com.ezur.ezur_web.repository.UsuarioRepository;

/** Carga las opciones de las listas desplegables la primera vez que la tabla está vacía. */
@Component
public class DatosIniciales implements CommandLineRunner {

    private final TipoServicioRepository tipoServicioRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${ezur.admin.username:admin}")
    private String adminUsername;

    @Value("${ezur.admin.password}")
    private String adminPassword;

    public DatosIniciales(TipoServicioRepository tipoServicioRepository, UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.tipoServicioRepository = tipoServicioRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (tipoServicioRepository.count() > 0) {
            return;
        }
        tipoServicioRepository.saveAll(List.of(
                new TipoServicio("Mudanza Local"),
                new TipoServicio("Mudanza Internacional"),
                new TipoServicio("Oficina / Comercio")));
        

        if (usuarioRepository.findByUsuario(adminUsername).isPresent()) {
            System.out.println("El administrador inicial ya existe.");
            return;
        }

        Usuario admin = new Usuario();
        admin.setDni("11111111");
        admin.setNombres("Admin");
        admin.setApellidos("Ezur");
        admin.setUsuario(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setPerfil(Perfil.ADMINISTRADOR);
        admin.setEstado(EstadoUsuario.ACTIVO);
        usuarioRepository.save(admin);
        System.out.println("Administrador inicial creado correctamente.");
    }
}
