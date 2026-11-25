package com.ezur.ezur_front.api;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ezur.ezur_front.dto.CambioClaveRequest;
import com.ezur.ezur_front.dto.ContactoRequest;
import com.ezur.ezur_front.dto.PresupuestoRequest;
import com.ezur.ezur_front.dto.RegistroRequest;
import com.ezur.ezur_front.dto.TestimonioRequest;
import com.ezur.ezur_front.dto.UsuarioRequest;
import com.ezur.ezur_front.model.Contacto;
import com.ezur.ezur_front.model.EstadoPresupuesto;
import com.ezur.ezur_front.model.EstadoUsuario;
import com.ezur.ezur_front.model.Presupuesto;
import com.ezur.ezur_front.model.Testimonio;
import com.ezur.ezur_front.model.TipoServicio;
import com.ezur.ezur_front.model.Usuario;
import com.ezur.ezur_front.security.UsuarioApi;

import jakarta.servlet.http.HttpServletRequest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Único punto por donde el frontend habla con la API REST (ezur-api).
 * Lleva el token de la sesión y la IP real del visitante, para que la API aplique sus límites por persona.
 */
@Component
public class ApiClient {

    public record LoginRespuesta(String token, String usuario, String perfil, boolean requiere2fa) {}

    private static final ParameterizedTypeReference<List<Presupuesto>> LISTA_PRESUPUESTOS = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Testimonio>> LISTA_TESTIMONIOS = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<TipoServicio>> LISTA_TIPOS = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Contacto>> LISTA_CONTACTOS = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<Usuario>> LISTA_USUARIOS = new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<Map<String, Object>> MAPA = new ParameterizedTypeReference<>() {};

    private final RestClient http;
    private final JsonMapper json = JsonMapper.builder().build();

    public ApiClient(RestClient.Builder builder, @Value("${api.url}") String urlBase) {
        this.http = builder
                .baseUrl(urlBase)
                .defaultStatusHandler(estado -> estado.isError(), (peticion, respuesta) -> {
                    throw traducir(respuesta.getStatusCode().value(), respuesta.getBody().readAllBytes());
                })
                .build();
    }

    // ---------- Acceso ----------

    public LoginRespuesta login(String usuario, String clave, String ambito) {
        return http.post().uri("/api/auth/login").headers(this::cabeceras)
                .body(Map.of("usuario", usuario, "clave", clave, "ambito", ambito == null ? "" : ambito))
                .retrieve().body(LoginRespuesta.class);
    }

    public void registrar(RegistroRequest request) {
        http.post().uri("/api/auth/registro").headers(this::cabeceras).body(request).retrieve().toBodilessEntity();
    }

    public Map<String, Object> estado2fa(String dispositivo, String secreto) {
        return http.get()
                .uri(u -> u.path("/api/auth/2fa").queryParamIfPresent("secreto", java.util.Optional.ofNullable(secreto)).build())
                .headers(h -> {
                    cabeceras(h);
                    if (dispositivo != null) {
                        h.set("X-Dispositivo", dispositivo);
                    }
                })
                .retrieve().body(MAPA);
    }

    public Map<String, Object> verificar2fa(String codigo, boolean recordar, String secreto) {
        Map<String, Object> cuerpo = new java.util.HashMap<>();
        cuerpo.put("codigo", codigo);
        cuerpo.put("recordar", recordar);
        cuerpo.put("secreto", secreto);
        return http.post().uri("/api/auth/2fa").headers(this::cabeceras).body(cuerpo).retrieve().body(MAPA);
    }

    public void olvidePassword(String email) {
        http.post().uri("/api/auth/olvide").headers(this::cabeceras)
                .body(Map.of("email", email)).retrieve().toBodilessEntity();
    }

    public boolean tokenDeRestablecerValido(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        Map<String, Object> r = http.get().uri("/api/auth/restablecer/{token}", token).headers(this::cabeceras)
                .retrieve().body(MAPA);
        return r != null && Boolean.TRUE.equals(r.get("valido"));
    }

    public void restablecerPassword(String token, String clave) {
        http.post().uri("/api/auth/restablecer").headers(this::cabeceras)
                .body(Map.of("token", token, "clave", clave)).retrieve().toBodilessEntity();
    }

    // ---------- Público ----------

    public List<Testimonio> testimonios() {
        return http.get().uri("/api/publico/testimonios").headers(this::cabeceras).retrieve().body(LISTA_TESTIMONIOS);
    }

    public List<TipoServicio> tiposServicio() {
        return http.get().uri("/api/publico/tipos-servicio").headers(this::cabeceras).retrieve().body(LISTA_TIPOS);
    }

    public Presupuesto solicitarPresupuesto(PresupuestoRequest request) {
        return http.post().uri("/api/presupuestos").headers(this::cabeceras).body(request)
                .retrieve().body(Presupuesto.class);
    }

    public void enviarContacto(ContactoRequest request) {
        http.post().uri("/api/contacto").headers(this::cabeceras).body(request).retrieve().toBodilessEntity();
    }

    // ---------- Cliente ----------

    public Usuario perfil() {
        return http.get().uri("/api/cliente/perfil").headers(this::cabeceras).retrieve().body(Usuario.class);
    }

    public List<Presupuesto> solicitudesDelCliente() {
        return http.get().uri("/api/cliente/solicitudes").headers(this::cabeceras).retrieve().body(LISTA_PRESUPUESTOS);
    }

    /** @return false si esa solicitud ya no se puede cancelar */
    public boolean cancelarSolicitud(Long id) {
        try {
            http.post().uri("/api/cliente/solicitudes/{id}/cancelar", id).headers(this::cabeceras)
                    .retrieve().toBodilessEntity();
            return true;
        } catch (ApiException e) {
            if (e.getEstado() == 409) {
                return false;
            }
            throw e;
        }
    }

    public void cambiarClave(CambioClaveRequest request) {
        http.post().uri("/api/cliente/clave").headers(this::cabeceras).body(request).retrieve().toBodilessEntity();
    }

    // ---------- Administración: solicitudes y mensajes ----------

    public List<Presupuesto> presupuestos() {
        return http.get().uri("/api/admin/presupuestos").headers(this::cabeceras).retrieve().body(LISTA_PRESUPUESTOS);
    }

    public Presupuesto presupuesto(Long id) {
        return http.get().uri("/api/admin/presupuestos/{id}", id).headers(this::cabeceras)
                .retrieve().body(Presupuesto.class);
    }

    public Presupuesto cambiarEstadoPresupuesto(Long id, EstadoPresupuesto estado) {
        return http.patch().uri("/api/admin/presupuestos/{id}/estado?estado={estado}", id, estado)
                .headers(this::cabeceras).retrieve().body(Presupuesto.class);
    }

    public void eliminarPresupuesto(Long id) {
        http.delete().uri("/api/admin/presupuestos/{id}", id).headers(this::cabeceras).retrieve().toBodilessEntity();
    }

    public List<Contacto> mensajes() {
        return http.get().uri("/api/admin/mensajes").headers(this::cabeceras).retrieve().body(LISTA_CONTACTOS);
    }

    public void eliminarMensaje(Long id) {
        http.delete().uri("/api/admin/mensajes/{id}", id).headers(this::cabeceras).retrieve().toBodilessEntity();
    }

    // ---------- Administración: testimonios ----------

    public List<Testimonio> testimoniosAdmin() {
        return http.get().uri("/api/admin/testimonios").headers(this::cabeceras).retrieve().body(LISTA_TESTIMONIOS);
    }

    public Testimonio testimonio(Long id) {
        return http.get().uri("/api/admin/testimonios/{id}", id).headers(this::cabeceras)
                .retrieve().body(Testimonio.class);
    }

    public Testimonio crearTestimonio(TestimonioRequest request) {
        return http.post().uri("/api/admin/testimonios").headers(this::cabeceras).body(request)
                .retrieve().body(Testimonio.class);
    }

    public Testimonio actualizarTestimonio(Long id, TestimonioRequest request) {
        return http.put().uri("/api/admin/testimonios/{id}", id).headers(this::cabeceras).body(request)
                .retrieve().body(Testimonio.class);
    }

    public void eliminarTestimonio(Long id) {
        http.delete().uri("/api/admin/testimonios/{id}", id).headers(this::cabeceras).retrieve().toBodilessEntity();
    }

    // ---------- Administración: usuarios ----------

    public List<Usuario> usuarios() {
        return http.get().uri("/api/admin/usuarios").headers(this::cabeceras).retrieve().body(LISTA_USUARIOS);
    }

    public Usuario usuario(Long id) {
        return http.get().uri("/api/admin/usuarios/{id}", id).headers(this::cabeceras).retrieve().body(Usuario.class);
    }

    public Usuario crearUsuario(UsuarioRequest request) {
        return http.post().uri("/api/admin/usuarios").headers(this::cabeceras).body(request)
                .retrieve().body(Usuario.class);
    }

    public Usuario actualizarUsuario(Long id, UsuarioRequest request) {
        return http.put().uri("/api/admin/usuarios/{id}", id).headers(this::cabeceras).body(request)
                .retrieve().body(Usuario.class);
    }

    public void cambiarEstadoUsuario(Long id, EstadoUsuario estado) {
        http.patch().uri("/api/admin/usuarios/{id}/estado?estado={estado}", id, estado)
                .headers(this::cabeceras).retrieve().toBodilessEntity();
    }

    public void eliminarUsuario(Long id) {
        http.delete().uri("/api/admin/usuarios/{id}", id).headers(this::cabeceras).retrieve().toBodilessEntity();
    }

    // ---------- Apoyo ----------

    /** Token de la sesión (si hay) e IP real del visitante. */
    private void cabeceras(HttpHeaders cabeceras) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioApi usuario && usuario.getToken() != null) {
            cabeceras.setBearerAuth(usuario.getToken());
        }
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            HttpServletRequest peticion = atributos.getRequest();
            cabeceras.set("X-Forwarded-For", peticion.getRemoteAddr());
        }
    }

    @SuppressWarnings("unchecked")
    private ApiException traducir(int estado, byte[] cuerpo) {
        String mensaje = "Error de la API (" + estado + ")";
        String codigo = null;
        Map<String, String> errores = null;
        try {
            Map<String, Object> datos = json.readValue(cuerpo, MAPA_JSON);
            if (datos.get("mensaje") instanceof String m) {
                mensaje = m;
            }
            if (datos.get("codigo") instanceof String c) {
                codigo = c;
            }
            if (datos.get("errores") instanceof Map<?, ?> e) {
                errores = (Map<String, String>) e;
            }
        } catch (RuntimeException e) {
            // Cuerpo vacío o que no es JSON (por ejemplo el 429 del limitador): queda el mensaje genérico
        }
        return new ApiException(estado, mensaje, codigo, errores);
    }

    private static final tools.jackson.core.type.TypeReference<Map<String, Object>> MAPA_JSON =
            new tools.jackson.core.type.TypeReference<>() {};
}
