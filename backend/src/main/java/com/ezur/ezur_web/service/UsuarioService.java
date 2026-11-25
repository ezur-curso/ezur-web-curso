package com.ezur.ezur_web.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;

import com.ezur.ezur_web.dto.RegistroRequest;
import com.ezur.ezur_web.dto.UsuarioRequest;
import com.ezur.ezur_web.exception.RecursoNoEncontradoException;
import com.ezur.ezur_web.model.EstadoUsuario;
import com.ezur.ezur_web.model.Usuario;
import com.ezur.ezur_web.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final int LARGO_MINIMO_CLAVE = 10;

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Usuario> listarTodos() {
        return repository.findAll();
    }

    public Usuario buscarPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }

    public Usuario buscarPorNombreUsuario(String nombreUsuario) {
        return repository.findByUsuario(nombreUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + nombreUsuario));
    }

    // ---- Registro público: el estado siempre arranca en PENDIENTE ----

    public void validarRegistro(RegistroRequest request, BindingResult result) {
        validarUnicos(request.getDni(), request.getUsuario(), null, result);
    }

    public Usuario registrar(RegistroRequest request) {
        Usuario usuario = new Usuario();
        usuario.setDni(request.getDni().trim());
        usuario.setNombres(request.getNombres().trim());
        usuario.setApellidos(request.getApellidos().trim());
        usuario.setUsuario(request.getUsuario().trim());
        usuario.setPassword(passwordEncoder.encode(request.getClave()));
        usuario.setPerfil(request.getPerfil());
        usuario.setEstado(EstadoUsuario.PENDIENTE);
        return guardar(usuario);
    }

    // ---- CRUD del administrador ----

    public void validarAdmin(UsuarioRequest request, Long idActual, BindingResult result) {
        validarUnicos(request.getDni(), request.getUsuario(), idActual, result);

        String clave = request.getClave();
        boolean hayClave = clave != null && !clave.isBlank();
        if (idActual == null && !hayClave) {
            result.rejectValue("clave", "clave.obligatoria", "La clave es obligatoria");
        } else if (hayClave && clave.length() < LARGO_MINIMO_CLAVE) {
            result.rejectValue("clave", "clave.corta", "La clave debe tener al menos 10 caracteres");
        }
    }

    public Usuario crear(UsuarioRequest request) {
        Usuario usuario = new Usuario();
        copiar(request, usuario);
        usuario.setPassword(passwordEncoder.encode(request.getClave()));
        return guardar(usuario);
    }

    public Usuario actualizar(Long id, UsuarioRequest request) {
        Usuario usuario = buscarPorId(id);
        copiar(request, usuario);
        if (request.getClave() != null && !request.getClave().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(request.getClave()));
        }
        return guardar(usuario);
    }

    /** Cambio de clave por el propio usuario: exige la clave actual. */
    public boolean cambiarClave(Usuario usuario, String claveActual, String claveNueva) {
        if (!passwordEncoder.matches(claveActual, usuario.getPassword())) {
            return false;
        }
        usuario.setPassword(passwordEncoder.encode(claveNueva));
        repository.save(usuario);
        return true;
    }

    public void cambiarEstado(Long id, EstadoUsuario estado) {
        Usuario usuario = buscarPorId(id);
        usuario.setEstado(estado);
        repository.save(usuario);
    }

    public void eliminar(Long id) {
        if (!repository.existsById(id)) {
            throw new RecursoNoEncontradoException("Usuario no encontrado: " + id);
        }
        repository.deleteById(id);
    }

    public UsuarioRequest aRequest(Usuario usuario) {
        UsuarioRequest request = new UsuarioRequest();
        request.setDni(usuario.getDni());
        request.setNombres(usuario.getNombres());
        request.setApellidos(usuario.getApellidos());
        request.setUsuario(usuario.getUsuario());
        request.setPerfil(usuario.getPerfil());
        request.setEstado(usuario.getEstado());
        return request;
    }

    private void copiar(UsuarioRequest request, Usuario usuario) {
        usuario.setDni(request.getDni().trim());
        usuario.setNombres(request.getNombres().trim());
        usuario.setApellidos(request.getApellidos().trim());
        usuario.setUsuario(request.getUsuario().trim());
        usuario.setPerfil(request.getPerfil());
        usuario.setEstado(request.getEstado());
    }

    private void validarUnicos(String dni, String nombreUsuario, Long idActual, BindingResult result) {
        String dniLimpio = dni == null ? "" : dni.trim();
        String usuarioLimpio = nombreUsuario == null ? "" : nombreUsuario.trim();

        boolean dniRepetido = idActual == null
                ? repository.existsByDni(dniLimpio)
                : repository.existsByDniAndIdNot(dniLimpio, idActual);
        if (dniRepetido && !result.hasFieldErrors("dni")) {
            result.rejectValue("dni", "dni.repetido", "Ya existe un usuario registrado con ese DNI");
        }

        boolean usuarioRepetido = idActual == null
                ? repository.existsByUsuario(usuarioLimpio)
                : repository.existsByUsuarioAndIdNot(usuarioLimpio, idActual);
        if (usuarioRepetido && !result.hasFieldErrors("usuario")) {
            result.rejectValue("usuario", "usuario.repetido", "Ese nombre de usuario ya está en uso");
        }
    }

    // Si dos personas envían el mismo DNI/usuario a la vez, la restricción única de la BD lo frena
    private Usuario guardar(Usuario usuario) {
        try {
            return repository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("El DNI o el usuario ya existen", e);
        }
    }
}
