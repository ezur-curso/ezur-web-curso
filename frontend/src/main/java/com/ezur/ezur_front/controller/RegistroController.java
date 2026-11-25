package com.ezur.ezur_front.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ezur.ezur_front.api.ApiClient;
import com.ezur.ezur_front.api.ApiException;
import com.ezur.ezur_front.api.ErroresApi;
import com.ezur.ezur_front.dto.RegistroRequest;
import com.ezur.ezur_front.model.Perfil;

import jakarta.validation.Valid;

@Controller
public class RegistroController {

    private static final Logger log = LoggerFactory.getLogger(RegistroController.class);

    private final ApiClient api;

    public RegistroController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/registro")
    public String formulario(Model model) {
        model.addAttribute("registro", new RegistroRequest());
        model.addAttribute("perfiles", Perfil.values());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrar(@Valid @ModelAttribute("registro") RegistroRequest request,
                            BindingResult result, Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                api.registrar(request);
            } catch (ApiException e) {
                if (!ErroresApi.aplicar(e, result)) {
                    throw e;
                }
            }
        }
        if (result.hasErrors()) {
            model.addAttribute("perfiles", Perfil.values());
            return "registro";
        }
        log.info("Registro enviado (pendiente de activación): usuario={}", request.getUsuario());
        redirect.addFlashAttribute("registrado", true);
        return "redirect:/login";
    }
}
