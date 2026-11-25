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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ezur.ezur_front.api.ApiClient;
import com.ezur.ezur_front.api.ApiException;
import com.ezur.ezur_front.api.ErroresApi;
import com.ezur.ezur_front.dto.PresupuestoRequest;
import com.ezur.ezur_front.model.EstadoPresupuesto;
import com.ezur.ezur_front.model.Presupuesto;

import jakarta.validation.Valid;

@Controller
public class PresupuestoController {

    private static final Logger log = LoggerFactory.getLogger(PresupuestoController.class);

    private final ApiClient api;

    public PresupuestoController(ApiClient api) {
        this.api = api;
    }

    @PostMapping("/cotizar")
    public String guardarPresupuesto(@Valid @ModelAttribute("presupuesto") PresupuestoRequest presupuestoRequest,
                                     BindingResult result, Model model, RedirectAttributes redirectAttributes,
                                     Authentication auth) {
        if (!result.hasErrors()) {
            try {
                api.solicitarPresupuesto(presupuestoRequest);
            } catch (ApiException e) {
                if (!ErroresApi.aplicar(e, result)) {
                    throw e;
                }
            }
        }
        if (result.hasErrors()) {
            model.addAttribute("tiposServicio", api.tiposServicio());
            return "cotizar";
        }
        redirectAttributes.addFlashAttribute("exito", true);
        // Con sesión de cliente, la solicitud quedó en su espacio "Mis mudanzas"
        boolean esCliente = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"));
        return esCliente ? "redirect:/cliente?solicitada" : "redirect:/cotizar";
    }

    @GetMapping("/admin")
    public String panelAdmin(Model model) {
        List<Presupuesto> presupuestos = api.presupuestos();
        model.addAttribute("presupuestos", presupuestos);
        model.addAttribute("totalSolicitudes", presupuestos.size());
        model.addAttribute("totalPendientes", contar(presupuestos, EstadoPresupuesto.PENDIENTE));
        model.addAttribute("totalContactados", contar(presupuestos, EstadoPresupuesto.CONTACTADO));
        model.addAttribute("totalConfirmados", contar(presupuestos, EstadoPresupuesto.CONFIRMADO));
        return "admin";
    }

    @PatchMapping("/admin/presupuestos/{id}/estado")
    @ResponseBody
    public Presupuesto cambiarEstado(@PathVariable Long id, @RequestParam EstadoPresupuesto estado) {
        return api.cambiarEstadoPresupuesto(id, estado);
    }

    @GetMapping("/admin/presupuestos/{id}")
    @ResponseBody
    public Presupuesto obtenerPresupuesto(@PathVariable Long id) {
        return api.presupuesto(id);
    }

    @PostMapping("/admin/presupuestos/{id}/eliminar")
    public String eliminarPresupuesto(@PathVariable Long id, Authentication auth,
                                      RedirectAttributes redirectAttributes) {
        api.eliminarPresupuesto(id);
        log.info("Presupuesto eliminado: id={} por={}", id, auth.getName());
        redirectAttributes.addFlashAttribute("eliminado", true);
        return "redirect:/admin";
    }

    private static long contar(List<Presupuesto> presupuestos, EstadoPresupuesto estado) {
        return presupuestos.stream().filter(p -> p.getEstado() == estado).count();
    }
}
