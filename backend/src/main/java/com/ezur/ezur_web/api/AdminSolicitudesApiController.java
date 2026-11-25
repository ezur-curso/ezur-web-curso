package com.ezur.ezur_web.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ezur.ezur_web.model.Contacto;
import com.ezur.ezur_web.model.EstadoPresupuesto;
import com.ezur.ezur_web.model.Presupuesto;
import com.ezur.ezur_web.service.ContactoService;
import com.ezur.ezur_web.service.PresupuestoService;

/** Panel del administrador: solicitudes de presupuesto y mensajes de contacto. */
@RestController
@RequestMapping("/api/admin")
public class AdminSolicitudesApiController {

    private static final Logger log = LoggerFactory.getLogger(AdminSolicitudesApiController.class);

    private final PresupuestoService presupuestoService;
    private final ContactoService contactoService;

    public AdminSolicitudesApiController(PresupuestoService presupuestoService, ContactoService contactoService) {
        this.presupuestoService = presupuestoService;
        this.contactoService = contactoService;
    }

    @GetMapping("/presupuestos")
    public List<Presupuesto> presupuestos() {
        return presupuestoService.listaTodos();
    }

    @GetMapping("/presupuestos/{id}")
    public Presupuesto presupuesto(@PathVariable Long id) {
        return presupuestoService.buscarPorId(id);
    }

    @PatchMapping("/presupuestos/{id}/estado")
    public Presupuesto cambiarEstado(@PathVariable Long id, @RequestParam EstadoPresupuesto estado) {
        return presupuestoService.actualizarEstado(id, estado);
    }

    @DeleteMapping("/presupuestos/{id}")
    public ResponseEntity<Void> eliminarPresupuesto(@PathVariable Long id, Authentication auth) {
        presupuestoService.eliminar(id);
        log.info("Presupuesto eliminado: id={} por={}", id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/mensajes")
    public List<Contacto> mensajes() {
        return contactoService.listarTodos();
    }

    @DeleteMapping("/mensajes/{id}")
    public ResponseEntity<Void> eliminarMensaje(@PathVariable Long id, Authentication auth) {
        contactoService.eliminar(id);
        log.info("Mensaje eliminado: id={} por={}", id, auth.getName());
        return ResponseEntity.noContent().build();
    }
}
