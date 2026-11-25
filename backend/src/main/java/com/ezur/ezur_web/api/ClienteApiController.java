package com.ezur.ezur_web.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ezur.ezur_web.dto.CambioClaveRequest;
import com.ezur.ezur_web.model.Presupuesto;
import com.ezur.ezur_web.model.Usuario;
import com.ezur.ezur_web.service.PresupuestoService;
import com.ezur.ezur_web.service.UsuarioService;

import jakarta.validation.Valid;

/** Espacio del cliente: su cuenta, sus solicitudes y el cambio de clave. */
@RestController
@RequestMapping("/api/cliente")
public class ClienteApiController {

    private static final Logger log = LoggerFactory.getLogger(ClienteApiController.class);

    private final UsuarioService usuarioService;
    private final PresupuestoService presupuestoService;

    public ClienteApiController(UsuarioService usuarioService, PresupuestoService presupuestoService) {
        this.usuarioService = usuarioService;
        this.presupuestoService = presupuestoService;
    }

    @GetMapping("/perfil")
    public UsuarioDto perfil(Authentication auth) {
        return UsuarioDto.de(usuarioService.buscarPorNombreUsuario(auth.getName()));
    }

    @GetMapping("/solicitudes")
    public List<Presupuesto> solicitudes(Authentication auth) {
        Usuario cliente = usuarioService.buscarPorNombreUsuario(auth.getName());
        return presupuestoService.listarDelCliente(cliente.getId());
    }

    @PostMapping("/solicitudes/{id}/cancelar")
    public ResponseEntity<Void> cancelar(@PathVariable Long id, Authentication auth) {
        Usuario cliente = usuarioService.buscarPorNombreUsuario(auth.getName());
        if (!presupuestoService.cancelarDelCliente(id, cliente.getId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        log.info("Solicitud {} cancelada por su cliente {}", id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/clave")
    public ResponseEntity<Void> cambiarClave(@Valid @RequestBody CambioClaveRequest request, Authentication auth) {
        Usuario cliente = usuarioService.buscarPorNombreUsuario(auth.getName());
        var result = new BeanPropertyBindingResult(request, "cambioClave");
        if (!request.getNueva().equals(request.getConfirmacion())) {
            result.rejectValue("confirmacion", "clave.distinta", "Las claves nuevas no coinciden");
        }
        ValidacionException.siHayErrores(result);
        if (!usuarioService.cambiarClave(cliente, request.getActual(), request.getNueva())) {
            throw new ValidacionException("actual", "La clave actual no es correcta");
        }
        log.info("Cliente {} cambió su clave", auth.getName());
        return ResponseEntity.noContent().build();
    }
}
