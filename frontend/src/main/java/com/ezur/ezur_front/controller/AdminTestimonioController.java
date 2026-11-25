package com.ezur.ezur_front.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ezur.ezur_front.api.ApiClient;
import com.ezur.ezur_front.api.ApiException;
import com.ezur.ezur_front.api.ErroresApi;
import com.ezur.ezur_front.dto.TestimonioRequest;
import com.ezur.ezur_front.model.Testimonio;
import com.ezur.ezur_front.model.TipoServicio;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/testimonios")
public class AdminTestimonioController {

    private static final Logger log = LoggerFactory.getLogger(AdminTestimonioController.class);

    private final ApiClient api;

    public AdminTestimonioController(ApiClient api) {
        this.api = api;
    }

    // READ: lista
    @GetMapping
    public String listar(Model model) {
        model.addAttribute("testimonios", api.testimoniosAdmin());
        return "admin-testimonios";
    }

    // CREATE: mostrar formulario vacío
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        prepararFormulario(model, new TestimonioRequest(), "Nuevo testimonio", "/admin/testimonios");
        return "admin-testimonio-form";
    }

    // CREATE: guardar
    @PostMapping
    public String crear(@Valid @ModelAttribute("testimonio") TestimonioRequest request,
                        BindingResult result, Model model,
                        RedirectAttributes redirect, Authentication auth) {
        if (!result.hasErrors()) {
            try {
                Testimonio creado = api.crearTestimonio(request);
                log.info("Testimonio {} creado por {}", creado.getId(), auth.getName());
            } catch (ApiException e) {
                if (!ErroresApi.aplicar(e, result)) {
                    throw e;
                }
            }
        }
        if (result.hasErrors()) {
            cargarServicios(model, request.getServicio());
            model.addAttribute("titulo", "Nuevo testimonio");
            model.addAttribute("accion", "/admin/testimonios");
            return "admin-testimonio-form";
        }
        redirect.addFlashAttribute("exito", "Testimonio creado correctamente");
        return "redirect:/admin/testimonios";
    }

    // UPDATE: formulario
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        prepararFormulario(model, aRequest(api.testimonio(id)), "Editar testimonio", "/admin/testimonios/" + id);
        return "admin-testimonio-form";
    }

    // UPDATE: guardar
    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("testimonio") TestimonioRequest request,
                             BindingResult result, Model model,
                             RedirectAttributes redirect, Authentication auth) {
        if (!result.hasErrors()) {
            try {
                api.actualizarTestimonio(id, request);
                log.info("Testimonio {} actualizado por {}", id, auth.getName());
            } catch (ApiException e) {
                if (!ErroresApi.aplicar(e, result)) {
                    throw e;
                }
            }
        }
        if (result.hasErrors()) {
            cargarServicios(model, request.getServicio());
            model.addAttribute("titulo", "Editar testimonio");
            model.addAttribute("accion", "/admin/testimonios/" + id);
            return "admin-testimonio-form";
        }
        redirect.addFlashAttribute("exito", "Testimonio actualizado correctamente");
        return "redirect:/admin/testimonios";
    }

    // DELETE
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect, Authentication auth) {
        api.eliminarTestimonio(id);
        log.info("Testimonio {} eliminado por {}", id, auth.getName());
        redirect.addFlashAttribute("eliminado", true);
        return "redirect:/admin/testimonios";
    }

    private void cargarServicios(Model model, String servicioActual) {
        List<TipoServicio> tipos = api.tiposServicio();
        model.addAttribute("tiposServicio", tipos);
        // Testimonios antiguos con un servicio que no está en la tabla: se conserva como opción extra
        boolean fueraDeLista = servicioActual != null && !servicioActual.isBlank()
                && tipos.stream().noneMatch(t -> servicioActual.equals(t.getNombre()));
        model.addAttribute("servicioExtra", fueraDeLista ? servicioActual : null);
    }

    private void prepararFormulario(Model model, TestimonioRequest request, String titulo, String accion) {
        model.addAttribute("testimonio", request);
        cargarServicios(model, request.getServicio());
        model.addAttribute("titulo", titulo);
        model.addAttribute("accion", accion);
    }

    private TestimonioRequest aRequest(Testimonio testimonio) {
        TestimonioRequest request = new TestimonioRequest();
        request.setNombre(testimonio.getNombre());
        request.setTexto(testimonio.getTexto());
        request.setServicio(testimonio.getServicio());
        try {
            request.setEstrellas(Integer.valueOf(testimonio.getEstrellas().trim()));
        } catch (RuntimeException e) {
            request.setEstrellas(5);
        }
        return request;
    }
}
