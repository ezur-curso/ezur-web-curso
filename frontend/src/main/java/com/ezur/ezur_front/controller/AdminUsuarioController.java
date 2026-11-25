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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ezur.ezur_front.api.ApiClient;
import com.ezur.ezur_front.api.ApiException;
import com.ezur.ezur_front.api.ErroresApi;
import com.ezur.ezur_front.dto.UsuarioRequest;
import com.ezur.ezur_front.model.EstadoUsuario;
import com.ezur.ezur_front.model.Perfil;
import com.ezur.ezur_front.model.Usuario;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/usuarios")
public class AdminUsuarioController {

    private static final Logger log = LoggerFactory.getLogger(AdminUsuarioController.class);

    private final ApiClient api;

    public AdminUsuarioController(ApiClient api) {
        this.api = api;
    }

    // READ
    @GetMapping
    public String listar(Model model, Authentication auth) {
        model.addAttribute("usuarios", api.usuarios());
        model.addAttribute("actual", auth.getName());
        return "admin-usuarios";
    }

    // CREATE: formulario
    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        prepararFormulario(model, new UsuarioRequest(), "Nuevo usuario", "/admin/usuarios", true);
        return "admin-usuario-form";
    }

    // CREATE: guardar
    @PostMapping
    public String crear(@Valid @ModelAttribute("usuario") UsuarioRequest request,
                        BindingResult result, Model model,
                        RedirectAttributes redirect, Authentication auth) {
        if (!result.hasErrors()) {
            try {
                Usuario creado = api.crearUsuario(request);
                log.info("Usuario {} creado por {}", creado.getId(), auth.getName());
            } catch (ApiException e) {
                if (!ErroresApi.aplicar(e, result)) {
                    throw e;
                }
            }
        }
        if (result.hasErrors()) {
            prepararFormulario(model, request, "Nuevo usuario", "/admin/usuarios", true);
            return "admin-usuario-form";
        }
        redirect.addFlashAttribute("exito", "Usuario creado correctamente");
        return "redirect:/admin/usuarios";
    }

    // UPDATE: formulario
    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        prepararFormulario(model, aRequest(api.usuario(id)), "Editar usuario", "/admin/usuarios/" + id, false);
        return "admin-usuario-form";
    }

    // UPDATE: guardar
    @PostMapping("/{id}")
    public String actualizar(@PathVariable Long id,
                             @Valid @ModelAttribute("usuario") UsuarioRequest request,
                             BindingResult result, Model model,
                             RedirectAttributes redirect, Authentication auth) {
        if (!result.hasErrors()) {
            try {
                api.actualizarUsuario(id, request);
                log.info("Usuario {} actualizado por {}", id, auth.getName());
            } catch (ApiException e) {
                if (!ErroresApi.aplicar(e, result)) {
                    throw e;
                }
            }
        }
        if (result.hasErrors()) {
            prepararFormulario(model, request, "Editar usuario", "/admin/usuarios/" + id, false);
            return "admin-usuario-form";
        }
        redirect.addFlashAttribute("exito", "Usuario actualizado correctamente");
        return "redirect:/admin/usuarios";
    }

    // Activar / anular / dejar pendiente con un clic desde la lista
    @PostMapping("/{id}/estado")
    public String cambiarEstado(@PathVariable Long id, @RequestParam EstadoUsuario estado,
                                RedirectAttributes redirect, Authentication auth) {
        try {
            api.cambiarEstadoUsuario(id, estado);
        } catch (ApiException e) {
            return rechazar(e, redirect);
        }
        log.info("Usuario {} pasó a {} por {}", id, estado, auth.getName());
        redirect.addFlashAttribute("exito", "Estado actualizado: " + estado.getEtiqueta());
        return "redirect:/admin/usuarios";
    }

    // DELETE
    @PostMapping("/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirect, Authentication auth) {
        try {
            api.eliminarUsuario(id);
        } catch (ApiException e) {
            return rechazar(e, redirect);
        }
        log.info("Usuario {} eliminado por {}", id, auth.getName());
        redirect.addFlashAttribute("exito", "Usuario eliminado correctamente");
        return "redirect:/admin/usuarios";
    }

    // La API no deja que un administrador se desactive o elimine a sí mismo: se muestra su motivo
    private String rechazar(ApiException e, RedirectAttributes redirect) {
        if (e.getEstado() != 400 || e.getErrores().isEmpty()) {
            throw e;
        }
        redirect.addFlashAttribute("error", e.getErrores().values().iterator().next());
        return "redirect:/admin/usuarios";
    }

    private UsuarioRequest aRequest(Usuario usuario) {
        UsuarioRequest request = new UsuarioRequest();
        request.setDni(usuario.getDni());
        request.setNombres(usuario.getNombres());
        request.setApellidos(usuario.getApellidos());
        request.setUsuario(usuario.getUsuario());
        request.setPerfil(usuario.getPerfil());
        request.setEstado(usuario.getEstado());
        return request;
    }

    private void prepararFormulario(Model model, UsuarioRequest request, String titulo,
                                    String accion, boolean esNuevo) {
        model.addAttribute("usuario", request);
        model.addAttribute("titulo", titulo);
        model.addAttribute("accion", accion);
        model.addAttribute("esNuevo", esNuevo);
        model.addAttribute("perfiles", Perfil.values());
        model.addAttribute("estados", EstadoUsuario.values());
    }
}
