package com.ezur.ezur_web.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ezur.ezur_web.dto.UsuarioRequest;
import com.ezur.ezur_web.model.EstadoUsuario;
import com.ezur.ezur_web.model.Perfil;
import com.ezur.ezur_web.model.Usuario;
import com.ezur.ezur_web.service.UsuarioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/usuarios")
public class AdminUsuarioApiController {

    private static final Logger log = LoggerFactory.getLogger(AdminUsuarioApiController.class);

    private final UsuarioService service;

    public AdminUsuarioApiController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return service.listarTodos().stream().map(UsuarioDto::de).toList();
    }

    @GetMapping("/{id}")
    public UsuarioDto obtener(@PathVariable Long id) {
        return UsuarioDto.de(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioDto> crear(@Valid @RequestBody UsuarioRequest request, Authentication auth) {
        var result = new BeanPropertyBindingResult(request, "usuario");
        service.validarAdmin(request, null, result);
        ValidacionException.siHayErrores(result);
        Usuario creado = service.crear(request);
        log.info("Usuario {} creado por {}", creado.getId(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioDto.de(creado));
    }

    @PutMapping("/{id}")
    public UsuarioDto actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request,
                                 Authentication auth) {
        Usuario existente = service.buscarPorId(id);
        var result = new BeanPropertyBindingResult(request, "usuario");
        service.validarAdmin(request, id, result);
        validarAutoEdicion(existente, request, auth, result);
        ValidacionException.siHayErrores(result);
        Usuario actualizado = service.actualizar(id, request);
        log.info("Usuario {} actualizado por {}", id, auth.getName());
        return UsuarioDto.de(actualizado);
    }

    // Activar / anular / dejar pendiente con un clic desde la lista
    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestParam EstadoUsuario estado,
                                           Authentication auth) {
        Usuario usuario = service.buscarPorId(id);
        if (usuario.getUsuario().equals(auth.getName()) && estado != EstadoUsuario.ACTIVO) {
            throw new ValidacionException("estado", "No puedes desactivar tu propia cuenta");
        }
        service.cambiarEstado(id, estado);
        log.info("Usuario {} pasó a {} por {}", id, estado, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        Usuario usuario = service.buscarPorId(id);
        if (usuario.getUsuario().equals(auth.getName())) {
            throw new ValidacionException("usuario", "No puedes eliminar tu propia cuenta");
        }
        service.eliminar(id);
        log.info("Usuario {} eliminado por {}", id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    // Un administrador no puede quitarse a sí mismo el acceso (evita quedarse sin administradores)
    private void validarAutoEdicion(Usuario existente, UsuarioRequest request, Authentication auth,
                                    BeanPropertyBindingResult result) {
        if (!existente.getUsuario().equals(auth.getName())) {
            return;
        }
        if (request.getEstado() != EstadoUsuario.ACTIVO) {
            result.rejectValue("estado", "estado.propio", "No puedes desactivar tu propia cuenta");
        }
        if (request.getPerfil() != Perfil.ADMINISTRADOR) {
            result.rejectValue("perfil", "perfil.propio", "No puedes quitarte el perfil de administrador");
        }
    }
}
