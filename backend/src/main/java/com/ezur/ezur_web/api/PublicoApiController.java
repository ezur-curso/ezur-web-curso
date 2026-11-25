package com.ezur.ezur_web.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.ezur.ezur_web.dto.ContactoRequest;
import com.ezur.ezur_web.dto.PresupuestoRequest;
import com.ezur.ezur_web.model.Presupuesto;
import com.ezur.ezur_web.model.Testimonio;
import com.ezur.ezur_web.model.TipoServicio;
import com.ezur.ezur_web.repository.TipoServicioRepository;
import com.ezur.ezur_web.service.ContactoService;
import com.ezur.ezur_web.service.EmailService;
import com.ezur.ezur_web.service.PresupuestoService;
import com.ezur.ezur_web.service.TestimonioService;
import com.ezur.ezur_web.service.UsuarioService;

import jakarta.validation.Valid;

/** Lo que el sitio público necesita sin iniciar sesión: opiniones, listas, presupuestos y contacto. */
@RestController
public class PublicoApiController {

    private static final Logger log = LoggerFactory.getLogger(PublicoApiController.class);

    private final TestimonioService testimonioService;
    private final TipoServicioRepository tipoServicioRepository;
    private final PresupuestoService presupuestoService;
    private final ContactoService contactoService;
    private final EmailService emailService;
    private final UsuarioService usuarioService;

    public PublicoApiController(TestimonioService testimonioService, TipoServicioRepository tipoServicioRepository,
                                PresupuestoService presupuestoService, ContactoService contactoService,
                                EmailService emailService, UsuarioService usuarioService) {
        this.testimonioService = testimonioService;
        this.tipoServicioRepository = tipoServicioRepository;
        this.presupuestoService = presupuestoService;
        this.contactoService = contactoService;
        this.emailService = emailService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/api/publico/testimonios")
    public List<Testimonio> testimonios() {
        return testimonioService.listarTodos();
    }

    @GetMapping("/api/publico/tipos-servicio")
    public List<TipoServicio> tiposServicio() {
        return tipoServicioRepository.findAllByOrderByIdAsc();
    }

    @PostMapping("/api/presupuestos")
    public ResponseEntity<Presupuesto> solicitarPresupuesto(@Valid @RequestBody PresupuestoRequest request,
                                                            Authentication auth) {
        String tipo = request.getTipoMudanza();
        if (tipo != null && !tipo.isBlank() && !tipoServicioRepository.existsByNombre(tipo)) {
            throw new ValidacionException("tipoMudanza", "Elige un tipo de mudanza de la lista");
        }
        // Con sesión de cliente, la solicitud queda en su espacio "Mis mudanzas"
        Long usuarioId = null;
        if (esCliente(auth)) {
            usuarioId = usuarioService.buscarPorNombreUsuario(auth.getName()).getId();
        }
        Presupuesto presupuesto = presupuestoService.guardarDesdeRequest(request, usuarioId);

        try {
            Map<String, Object> datos = new HashMap<>();
            datos.put("nombre", presupuesto.getNombreCompleto());
            datos.put("email", presupuesto.getEmail());
            datos.put("telefono", presupuesto.getTelefono());
            datos.put("origen", presupuesto.getDireccionOrigen());
            datos.put("destino", presupuesto.getDireccionDestino());
            datos.put("tipo", presupuesto.getTipoMudanza());
            datos.put("fecha", presupuesto.getFechaEstimada());
            datos.put("inventario", presupuesto.getComentarios());
            emailService.enviarNotificacionHtml("Nueva solicitud de presupuesto - EZUR", "presupuesto", datos);
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de notificación", e);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(presupuesto);
    }

    @PostMapping("/api/contacto")
    public ResponseEntity<Void> enviarContacto(@Valid @RequestBody ContactoRequest request) {
        var contacto = contactoService.guardarDesdeRequest(request);
        try {
            Map<String, Object> datos = new HashMap<>();
            datos.put("nombre", contacto.getNombre());
            datos.put("email", contacto.getEmail());
            datos.put("telefono", contacto.getTelefono());
            datos.put("asunto", contacto.getAsunto());
            datos.put("mensaje", contacto.getMensaje());
            emailService.enviarNotificacionHtml("Nuevo mensaje de contacto - EZUR", "contacto", datos);
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo de notificación", e);
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private static boolean esCliente(Authentication auth) {
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
    }
}
