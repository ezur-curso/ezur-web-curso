package com.ezur.ezur_front.controller;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ezur.ezur_front.api.ApiClient;
import com.ezur.ezur_front.api.ApiException;
import com.ezur.ezur_front.api.ErroresApi;
import com.ezur.ezur_front.dto.ContactoRequest;

import jakarta.validation.Valid;

@Controller
public class ContactoController {

    private static final Logger log = LoggerFactory.getLogger(ContactoController.class);

    private final ApiClient api;

    public ContactoController(ApiClient api) {
        this.api = api;
    }

    @PostMapping("/contacto")
    public String guardarContacto(@Valid @ModelAttribute("contacto") ContactoRequest contactoRequest,
                                  BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "contacto";
        }
        try {
            api.enviarContacto(contactoRequest);
        } catch (ApiException e) {
            if (!ErroresApi.aplicar(e, result)) {
                throw e;
            }
            return "contacto";
        }
        redirectAttributes.addFlashAttribute("exito", true);
        return "redirect:/contacto";
    }

    @GetMapping("/admin/mensajes")
    public String panelMensaje(Model model) {
        model.addAttribute("mensajes", api.mensajes());
        return "admin-mensajes";
    }

    @PostMapping("/admin/mensajes/{id}/eliminar")
    public String eliminarMensaje(@PathVariable Long id, Authentication auth, RedirectAttributes redirectAttributes) {
        api.eliminarMensaje(id);
        log.info("Mensaje eliminado: id={} por={}", id, auth.getName());
        redirectAttributes.addFlashAttribute("eliminado", true);
        return "redirect:/admin/mensajes";
    }
}
