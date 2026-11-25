package com.ezur.ezur_front;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ezur.ezur_front.api.ApiClient;
import com.ezur.ezur_front.api.ApiClient.LoginRespuesta;
import com.ezur.ezur_front.api.ApiException;
import com.ezur.ezur_front.model.EstadoUsuario;
import com.ezur.ezur_front.model.Perfil;
import com.ezur.ezur_front.model.Usuario;
import com.ezur.ezur_front.security.UsuarioApi;

@SpringBootTest
@AutoConfigureMockMvc
class AccesoTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ApiClient api;

    private static Usuario cliente() {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setNombres("Marta");
        u.setApellidos("Prueba");
        u.setNombreCompleto("Marta Prueba");
        u.setUsuario("marta");
        u.setDni("12345678");
        u.setPerfil(Perfil.CLIENTE);
        u.setEstado(EstadoUsuario.ACTIVO);
        return u;
    }

    // ---------- Quién llega adónde ----------

    @Test
    void sinSesionElAreaDeClientesPideElAccesoDeClientes() throws Exception {
        mvc.perform(get("/cliente")).andExpect(redirectedUrl("/login"));
    }

    @Test
    void sinSesionElPanelPideElAccesoDeAdministracion() throws Exception {
        mvc.perform(get("/admin")).andExpect(redirectedUrl("/admin-login"));
        mvc.perform(get("/admin/usuarios")).andExpect(redirectedUrl("/admin-login"));
    }

    @Test
    void elClienteNoEntraAlPanelDeAdministracion() throws Exception {
        mvc.perform(get("/admin").with(user(new UsuarioApi("marta", "CLIENTE", "tok"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdministradorSinSegundoFactorVaAlCodigo() throws Exception {
        mvc.perform(get("/admin").with(user(new UsuarioApi("jefe", "ADMINISTRADOR", "pre"))))
                .andExpect(redirectedUrl("/2fa"));
    }

    // ---------- Login contra la API ----------

    @Test
    void elClienteEntraConLosDatosQueLaApiAcepta() throws Exception {
        when(api.login(eq("marta"), eq("clave-segura-123"), any()))
                .thenReturn(new LoginRespuesta("tok", "marta", "CLIENTE", false));
        mvc.perform(post("/login").with(csrf()).param("username", "marta").param("password", "clave-segura-123"))
                .andExpect(redirectedUrl("/cliente"));
    }

    @Test
    void elAdministradorPasaPrimeroPorElSegundoFactor() throws Exception {
        when(api.login(eq("jefe"), eq("clave-segura-123"), eq("admin")))
                .thenReturn(new LoginRespuesta("pre", "jefe", "ADMINISTRADOR", true));
        mvc.perform(post("/login").with(csrf()).param("username", "jefe").param("password", "clave-segura-123")
                        .param("ambito", "admin"))
                .andExpect(redirectedUrl("/2fa"));
    }

    @Test
    void unaClaveIncorrectaVuelveAlLoginConError() throws Exception {
        when(api.login(any(), any(), any()))
                .thenThrow(new ApiException(401, "Error", "credenciales", null));
        mvc.perform(post("/login").with(csrf()).param("username", "marta").param("password", "mala"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void unaCuentaSinActivarSeAvisaAparte() throws Exception {
        when(api.login(any(), any(), any()))
                .thenThrow(new ApiException(403, "Error", "inactivo", null));
        mvc.perform(post("/login").with(csrf()).param("username", "marta").param("password", "x"))
                .andExpect(redirectedUrl("/login?inactivo"));
    }

    @Test
    void unaCuentaEnElAccesoEquivocadoVuelveAEseAcceso() throws Exception {
        when(api.login(any(), any(), any()))
                .thenThrow(new ApiException(403, "Error", "perfil", null));
        mvc.perform(post("/login").with(csrf()).param("username", "marta").param("password", "x")
                        .param("ambito", "admin"))
                .andExpect(redirectedUrl("/admin-login?perfil"));
    }

    @Test
    void siLaApiNoRespondeElLoginFallaSinCaerse() throws Exception {
        when(api.login(any(), any(), any()))
                .thenThrow(new org.springframework.web.client.ResourceAccessException("sin conexión"));
        mvc.perform(post("/login").with(csrf()).param("username", "marta").param("password", "x"))
                .andExpect(redirectedUrl("/login?error"));
    }

    // ---------- Área del cliente ----------

    @Test
    void elAreaDelClienteMuestraSusDatosDeLaApi() throws Exception {
        when(api.perfil()).thenReturn(cliente());
        when(api.solicitudesDelCliente()).thenReturn(List.of());
        mvc.perform(get("/cliente").with(user(new UsuarioApi("marta", "CLIENTE", "tok"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Marta")));
    }

    @Test
    void siElTokenVencioElClienteVuelveAEntrar() throws Exception {
        when(api.perfil()).thenThrow(new ApiException(401, "No autenticado", null, null));
        mvc.perform(get("/cliente").with(user(new UsuarioApi("marta", "CLIENTE", "vencido"))))
                .andExpect(redirectedUrl("/login?expirada"));
    }

    @Test
    void cancelarUnaSolicitudSeLoPideALaApi() throws Exception {
        when(api.cancelarSolicitud(7L)).thenReturn(true);
        mvc.perform(post("/cliente/solicitudes/7/cancelar").with(csrf())
                        .with(user(new UsuarioApi("marta", "CLIENTE", "tok"))))
                .andExpect(redirectedUrl("/cliente#mudanzas"));
    }

    // ---------- Registro ----------

    @Test
    void elRegistroMuestraElErrorDeUnDniRepetido() throws Exception {
        org.mockito.Mockito.doThrow(new ApiException(400, "Datos inválidos", null,
                java.util.Map.of("usuario", "El DNI o el usuario ya están registrados")))
                .when(api).registrar(any());
        mvc.perform(post("/registro").with(csrf())
                        .param("dni", "12345678").param("nombres", "Luis").param("apellidos", "Gomez")
                        .param("usuario", "luis.gomez").param("clave", "clave-larga-123").param("perfil", "CLIENTE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("El DNI o el usuario ya están registrados")));
    }
}
