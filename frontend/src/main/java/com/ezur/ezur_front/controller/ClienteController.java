package com.ezur.ezur_front.controller;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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
import com.ezur.ezur_front.dto.CambioClaveRequest;
import com.ezur.ezur_front.model.EstadoPresupuesto;
import com.ezur.ezur_front.model.Presupuesto;
import com.ezur.ezur_front.model.Usuario;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/cliente")
public class ClienteController {

    private static final Logger log = LoggerFactory.getLogger(ClienteController.class);

    private final ApiClient api;

    public ClienteController(ApiClient api) {
        this.api = api;
    }

    @GetMapping
    public String inicio(Model model) {
        cargarPanel(api.perfil(), model);
        if (!model.containsAttribute("cambioClave")) {
            model.addAttribute("cambioClave", new CambioClaveRequest());
        }
        return "cliente";
    }

    @PostMapping("/solicitudes/{id}/cancelar")
    public String cancelar(@PathVariable Long id, Authentication auth, RedirectAttributes redirect) {
        if (api.cancelarSolicitud(id)) {
            log.info("Solicitud {} cancelada por su cliente {}", id, auth.getName());
            redirect.addFlashAttribute("aviso", "Cancelaste la solicitud.");
        } else {
            redirect.addFlashAttribute("problema",
                    "Esa solicitud ya no se puede cancelar desde aquí. Escríbenos y la revisamos.");
        }
        return "redirect:/cliente#mudanzas";
    }

    @PostMapping("/clave")
    public String cambiarClave(@Valid @ModelAttribute("cambioClave") CambioClaveRequest request,
                               BindingResult result, Authentication auth,
                               Model model, RedirectAttributes redirect) {
        if (!result.hasErrors() && !request.getNueva().equals(request.getConfirmacion())) {
            result.rejectValue("confirmacion", "clave.distinta", "Las claves nuevas no coinciden");
        }
        if (!result.hasErrors()) {
            try {
                api.cambiarClave(request);
            } catch (ApiException e) {
                if (!ErroresApi.aplicar(e, result)) {
                    throw e;
                }
            }
        }
        if (result.hasErrors()) {
            cargarPanel(api.perfil(), model);
            model.addAttribute("abrirClave", true);
            return "cliente";
        }
        log.info("Cliente {} cambió su clave", auth.getName());
        redirect.addFlashAttribute("aviso", "Tu clave se actualizó.");
        return "redirect:/cliente#cuenta";
    }

    private void cargarPanel(Usuario cliente, Model model) {
        List<Presupuesto> solicitudes = api.solicitudesDelCliente();
        long enCurso = solicitudes.stream()
                .filter(s -> s.getEstado() == EstadoPresupuesto.PENDIENTE
                        || s.getEstado() == EstadoPresupuesto.CONTACTADO)
                .count();
        long confirmadas = solicitudes.stream()
                .filter(s -> s.getEstado() == EstadoPresupuesto.CONFIRMADO)
                .count();

        model.addAttribute("cliente", cliente);
        model.addAttribute("solicitudes", solicitudes);
        model.addAttribute("totalSolicitudes", solicitudes.size());
        model.addAttribute("enCurso", enCurso);
        model.addAttribute("confirmadas", confirmadas);

        // La mudanza más próxima que sigue viva; si ninguna tiene fecha futura, la última pedida
        LocalDate hoy = LocalDate.now();
        Presupuesto destacada = solicitudes.stream()
                .filter(s -> s.getEstado() != EstadoPresupuesto.CANCELADO)
                .filter(s -> s.getFechaEstimada() != null && !s.getFechaEstimada().isBefore(hoy))
                .min(Comparator.comparing(Presupuesto::getFechaEstimada))
                .orElseGet(() -> solicitudes.stream()
                        .filter(s -> s.getEstado() != EstadoPresupuesto.CANCELADO)
                        .findFirst().orElse(null));
        model.addAttribute("destacada", destacada);

        // Días que faltan para la mudanza destacada (solo si la fecha aún no pasó)
        Long diasRestantes = null;
        if (destacada != null && destacada.getFechaEstimada() != null
                && !destacada.getFechaEstimada().isBefore(hoy)) {
            diasRestantes = ChronoUnit.DAYS.between(hoy, destacada.getFechaEstimada());
        }
        model.addAttribute("diasRestantes", diasRestantes);
    }
}
