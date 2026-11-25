package com.ezur.ezur_front.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ezur.ezur_front.api.ApiClient;
import com.ezur.ezur_front.dto.ContactoRequest;
import com.ezur.ezur_front.dto.PresupuestoRequest;
import com.ezur.ezur_front.model.Testimonio;

@Controller
public class PageController {

    private final ApiClient api;

    public PageController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/")
    public String inicio(Model model) {
        List<Testimonio> testimonios = api.testimonios();
        model.addAttribute("testimonios", testimonios);
        model.addAttribute("tiposServicio", api.tiposServicio());

        // Nota media de las opiniones para el resumen de la portada
        double promedio = testimonios.stream()
                .mapToInt(t -> estrellas(t.getEstrellas()))
                .filter(e -> e > 0)
                .average().orElse(0);
        model.addAttribute("promedioOpiniones", String.format(Locale.forLanguageTag("es"), "%.1f", promedio));
        model.addAttribute("totalOpiniones", testimonios.size());
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/admin-login")
    public String loginAdmin() {
        return "admin-login";
    }

    @GetMapping("/servicios")
    public String servicios() {
        return "servicios";
    }

    @GetMapping("/nosotros")
    public String nosotros() {
        return "nosotros";
    }

    @GetMapping("/contacto")
    public String contacto(Model model) {
        model.addAttribute("contacto", new ContactoRequest());
        return "contacto";
    }

    @GetMapping("/cotizar")
    public String cotizar(Model model, Authentication auth,
                          @RequestParam(required = false) String origen,
                          @RequestParam(required = false) String destino,
                          @RequestParam(required = false) String tipo,
                          @RequestParam(required = false) String detalle) {
        PresupuestoRequest presupuesto = new PresupuestoRequest();
        // El mini formulario de la portada llega con estos datos ya escritos
        presupuesto.setDireccionOrigen(origen);
        presupuesto.setDireccionDestino(destino);
        presupuesto.setTipoMudanza(tipo);
        // La calculadora de la portada deja una nota con el tamaño estimado
        presupuesto.setComentarios(detalle);
        // Un cliente con sesión ya no reescribe su nombre
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CLIENTE"))) {
            presupuesto.setNombreCompleto(api.perfil().getNombreCompleto());
        }
        model.addAttribute("presupuesto", presupuesto);
        model.addAttribute("tiposServicio", api.tiposServicio());
        return "cotizar";
    }

    @GetMapping("/politica-privacidad")
    public String privacidad() {
        return "privacidad";
    }

    private static int estrellas(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (RuntimeException e) {
            return 0;
        }
    }
}
