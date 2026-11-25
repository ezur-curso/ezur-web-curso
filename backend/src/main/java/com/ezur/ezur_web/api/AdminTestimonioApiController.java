package com.ezur.ezur_web.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ezur.ezur_web.dto.TestimonioRequest;
import com.ezur.ezur_web.model.Testimonio;
import com.ezur.ezur_web.repository.TipoServicioRepository;
import com.ezur.ezur_web.service.TestimonioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/testimonios")
public class AdminTestimonioApiController {

    private static final Logger log = LoggerFactory.getLogger(AdminTestimonioApiController.class);

    private final TestimonioService service;
    private final TipoServicioRepository tipoServicioRepository;

    public AdminTestimonioApiController(TestimonioService service, TipoServicioRepository tipoServicioRepository) {
        this.service = service;
        this.tipoServicioRepository = tipoServicioRepository;
    }

    @GetMapping
    public List<Testimonio> listar() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public Testimonio obtener(@PathVariable Long id) {
        return service.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<Testimonio> crear(@Valid @RequestBody TestimonioRequest request, Authentication auth) {
        validarServicio(request, null);
        Testimonio creado = service.crear(request);
        log.info("Testimonio {} creado por {}", creado.getId(), auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public Testimonio actualizar(@PathVariable Long id, @Valid @RequestBody TestimonioRequest request,
                                 Authentication auth) {
        validarServicio(request, service.buscarPorId(id).getServicio());
        Testimonio actualizado = service.actualizar(id, request);
        log.info("Testimonio {} actualizado por {}", id, auth.getName());
        return actualizado;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Authentication auth) {
        service.eliminar(id);
        log.info("Testimonio {} eliminado por {}", id, auth.getName());
        return ResponseEntity.noContent().build();
    }

    // El servicio sale de la tabla tipos_servicio; se tolera el valor que el testimonio ya tenía guardado
    private void validarServicio(TestimonioRequest request, String servicioActual) {
        String servicio = request.getServicio();
        if (servicio == null || servicio.isBlank() || servicio.equals(servicioActual)) {
            return;
        }
        if (!tipoServicioRepository.existsByNombre(servicio)) {
            throw new ValidacionException("servicio", "Elige un servicio de la lista");
        }
    }
}
