package com.ezur.ezur_front.controller;

import static com.ezur.ezur_front.security.SegundoFactorFilter.SESION_2FA_OK;
import static com.ezur.ezur_front.security.SegundoFactorFilter.SESION_SECRETO_NUEVO;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ezur.ezur_front.api.ApiClient;
import com.ezur.ezur_front.api.ApiException;
import com.ezur.ezur_front.security.UsuarioApi;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Segundo factor del administrador. La API decide (y guarda el secreto); el frontend solo
 * conserva en la sesión que el código ya se verificó y la cookie del dispositivo de confianza.
 */
@Controller
public class TwoFactorController {

    private static final String COOKIE_DISPOSITIVO = "EZUR_DEVICE";
    private static final int DIAS_DE_VALIDEZ = 30;

    private final ApiClient api;

    @Value("${app.base-url}")
    private String baseUrl;

    public TwoFactorController(ApiClient api) {
        this.api = api;
    }

    @GetMapping("/2fa")
    public String mostrar(Authentication auth, HttpSession sesion, HttpServletRequest request, Model model) {
        if (auth.getName().equals(sesion.getAttribute(SESION_2FA_OK))) {
            return "redirect:/admin";
        }
        Map<String, Object> estado = api.estado2fa(leerCookie(request), (String) sesion.getAttribute(SESION_SECRETO_NUEVO));

        if ("OK".equals(estado.get("estado"))) {
            // Dispositivo de confianza: la API ya entrega el token con permiso de administración
            completar(auth, (String) estado.get("token"), sesion, request);
            return "redirect:/admin";
        }
        if ("CONFIGURAR".equals(estado.get("estado"))) {
            // Primera vez: el secreto se guarda solo en la sesión hasta que el código lo confirme
            sesion.setAttribute(SESION_SECRETO_NUEVO, estado.get("secreto"));
            model.addAttribute("qr", estado.get("qr"));
        }
        return "2fa";
    }

    @PostMapping("/2fa")
    public String verificar(@RequestParam String codigo,
                            @RequestParam(required = false) String recordar,
                            Authentication auth, HttpSession sesion,
                            HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> respuesta;
        try {
            respuesta = api.verificar2fa(codigo, recordar != null, (String) sesion.getAttribute(SESION_SECRETO_NUEVO));
        } catch (ApiException e) {
            if (e.getEstado() == 401) {
                return "redirect:/2fa?error";
            }
            throw e;
        }
        sesion.removeAttribute(SESION_SECRETO_NUEVO);
        completar(auth, (String) respuesta.get("token"), sesion, request);
        if (respuesta.get("dispositivo") instanceof String dispositivo) {
            guardarCookie(response, dispositivo);
        }
        return "redirect:/admin";
    }

    private void completar(Authentication auth, String token, HttpSession sesion, HttpServletRequest request) {
        ((UsuarioApi) auth.getPrincipal()).setToken(token);
        request.changeSessionId(); // pulsera nueva al subir de nivel
        sesion.setAttribute(SESION_2FA_OK, auth.getName());
    }

    private String leerCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (COOKIE_DISPOSITIVO.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    private void guardarCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(COOKIE_DISPOSITIVO, token);
        cookie.setHttpOnly(true);                       // JavaScript no puede leerla
        cookie.setSecure(baseUrl.startsWith("https"));  // en producción solo viaja por HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(DIAS_DE_VALIDEZ * 24 * 60 * 60);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }
}
