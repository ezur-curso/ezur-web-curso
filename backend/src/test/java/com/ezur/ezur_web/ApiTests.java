package com.ezur.ezur_web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.ezur.ezur_web.model.EstadoPresupuesto;
import com.ezur.ezur_web.model.EstadoUsuario;
import com.ezur.ezur_web.model.Perfil;
import com.ezur.ezur_web.model.Presupuesto;
import com.ezur.ezur_web.model.Usuario;
import com.ezur.ezur_web.repository.PresupuestoRepository;
import com.ezur.ezur_web.repository.UsuarioRepository;
import com.ezur.ezur_web.service.EmailService;
import com.jayway.jsonpath.JsonPath;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiTests {

    private static final String CLAVE = "clave-segura-123";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private PresupuestoRepository presupuestos;

    @Autowired
    private PasswordEncoder encoder;

    @MockitoBean
    private EmailService emailService;

    private static final AtomicInteger contador = new AtomicInteger(1);

    // El limitador de intentos cuenta por IP: cada petición declara una IP distinta
    private MockHttpServletRequestBuilder desdeOtraIp(MockHttpServletRequestBuilder peticion) {
        int n = contador.incrementAndGet();
        return peticion.header("X-Forwarded-For", "10.9." + n / 250 + "." + n % 250);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder peticion, String cuerpo) {
        return desdeOtraIp(peticion).contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    private Usuario crearUsuario(String nombreUsuario, Perfil perfil, EstadoUsuario estado) {
        Usuario u = new Usuario();
        u.setDni(String.format("%08d", Math.abs(nombreUsuario.hashCode()) % 100_000_000));
        u.setNombres("Marta");
        u.setApellidos("Prueba");
        u.setUsuario(nombreUsuario);
        u.setPassword(encoder.encode(CLAVE));
        u.setPerfil(perfil);
        u.setEstado(estado);
        return usuarios.save(u);
    }

    private String login(String usuario, String clave, String ambito) throws Exception {
        String respuesta = mvc.perform(json(post("/api/auth/login"),
                        "{\"usuario\":\"" + usuario + "\",\"clave\":\"" + clave + "\",\"ambito\":\"" + ambito + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    /** Un administrador con 2FA ya configurado entra con contraseña y código. */
    private String tokenAdmin(String nombreUsuario) throws Exception {
        Usuario admin = crearUsuario(nombreUsuario, Perfil.ADMINISTRADOR, EstadoUsuario.ACTIVO);
        String secreto = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP";
        admin.setSecretoTotp(secreto);
        usuarios.save(admin);

        String pre = login(nombreUsuario, CLAVE, "admin");
        String codigo = new DefaultCodeGenerator().generate(secreto,
                Math.floorDiv(new SystemTimeProvider().getTime(), 30));
        String respuesta = mvc.perform(json(post("/api/auth/2fa"), "{\"codigo\":\"" + codigo + "\"}")
                        .header("Authorization", "Bearer " + pre))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.token");
    }

    private static String solicitudJson(String tipo) {
        return "{\"nombreCompleto\":\"Marta Prueba\",\"email\":\"marta@correo.com\",\"telefono\":\"600000000\","
                + "\"direccionOrigen\":\"Calle Origen 1\",\"direccionDestino\":\"Calle Destino 2\","
                + "\"tipoMudanza\":\"" + tipo + "\",\"aceptaPrivacidad\":true}";
    }

    // ---------- Público ----------

    @Test
    void laListaDeServiciosEsPublica() throws Exception {
        mvc.perform(get("/api/publico/tipos-servicio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nombre", hasItem("Mudanza Local")));
    }

    @Test
    void sinTokenLasZonasProtegidasDanNoAutenticado() throws Exception {
        mvc.perform(get("/api/admin/usuarios")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/cliente/solicitudes")).andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenFalsoNoSirve() throws Exception {
        mvc.perform(get("/api/cliente/solicitudes").header("Authorization", "Bearer a.b.c"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unaRutaQueNoExisteNoSePuedeUsarSinPermiso() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
    }

    @Test
    void cualquieraPuedePedirUnPresupuesto() throws Exception {
        mvc.perform(json(post("/api/presupuestos"), solicitudJson("Mudanza Local")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    void elPresupuestoRechazaUnTipoQueNoEstaEnLaLista() throws Exception {
        mvc.perform(json(post("/api/presupuestos"), solicitudJson("Viaje a la luna")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.tipoMudanza").exists());
    }

    @Test
    void elPresupuestoExigeAceptarLaPrivacidad() throws Exception {
        mvc.perform(json(post("/api/presupuestos"), solicitudJson("Mudanza Local").replace("true", "false")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.aceptaPrivacidad").exists());
    }

    @Test
    void elContactoSeGuarda() throws Exception {
        mvc.perform(json(post("/api/contacto"),
                        "{\"nombre\":\"Ana\",\"email\":\"ana@correo.com\",\"mensaje\":\"Hola\",\"aceptaPrivacidad\":true}"))
                .andExpect(status().isCreated());
    }

    // ---------- Acceso ----------

    @Test
    void elClienteActivoEntraYVeSuPerfil() throws Exception {
        crearUsuario("cliente.login", Perfil.CLIENTE, EstadoUsuario.ACTIVO);
        String token = login("cliente.login", CLAVE, "");
        mvc.perform(get("/api/cliente/perfil").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario").value("cliente.login"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("password"))));
    }

    @Test
    void unaClaveIncorrectaDaCredenciales() throws Exception {
        crearUsuario("cliente.mal", Perfil.CLIENTE, EstadoUsuario.ACTIVO);
        mvc.perform(json(post("/api/auth/login"), "{\"usuario\":\"cliente.mal\",\"clave\":\"otra\",\"ambito\":\"\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("credenciales"));
    }

    @Test
    void unaCuentaPendienteNoEntra() throws Exception {
        crearUsuario("cliente.pendiente", Perfil.CLIENTE, EstadoUsuario.PENDIENTE);
        mvc.perform(json(post("/api/auth/login"),
                        "{\"usuario\":\"cliente.pendiente\",\"clave\":\"" + CLAVE + "\",\"ambito\":\"\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("inactivo"));
    }

    @Test
    void cadaCuentaEntraSoloPorSuAcceso() throws Exception {
        crearUsuario("cliente.ambito", Perfil.CLIENTE, EstadoUsuario.ACTIVO);
        crearUsuario("admin.ambito", Perfil.ADMINISTRADOR, EstadoUsuario.ACTIVO);
        mvc.perform(json(post("/api/auth/login"),
                        "{\"usuario\":\"cliente.ambito\",\"clave\":\"" + CLAVE + "\",\"ambito\":\"admin\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("perfil"));
        mvc.perform(json(post("/api/auth/login"),
                        "{\"usuario\":\"admin.ambito\",\"clave\":\"" + CLAVE + "\",\"ambito\":\"\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("perfil"));
    }

    @Test
    void elClienteNoEntraAlPanelDeAdministracion() throws Exception {
        crearUsuario("cliente.nosoy", Perfil.CLIENTE, EstadoUsuario.ACTIVO);
        String token = login("cliente.nosoy", CLAVE, "");
        mvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void elAdministradorNecesitaElSegundoFactor() throws Exception {
        Usuario admin = crearUsuario("admin.dosfa", Perfil.ADMINISTRADOR, EstadoUsuario.ACTIVO);
        admin.setSecretoTotp("JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP");
        usuarios.save(admin);

        String pre = login("admin.dosfa", CLAVE, "admin");
        // Con solo la contraseña no llega al panel
        mvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + pre))
                .andExpect(status().isForbidden());
        // Un código equivocado no sube de nivel
        mvc.perform(json(post("/api/auth/2fa"), "{\"codigo\":\"000000\"}").header("Authorization", "Bearer " + pre))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void laPrimeraVezElSegundoFactorSePideConfigurar() throws Exception {
        crearUsuario("admin.nuevo", Perfil.ADMINISTRADOR, EstadoUsuario.ACTIVO);
        String pre = login("admin.nuevo", CLAVE, "admin");
        mvc.perform(get("/api/auth/2fa").header("Authorization", "Bearer " + pre))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIGURAR"))
                .andExpect(jsonPath("$.secreto").exists())
                .andExpect(jsonPath("$.qr").exists());
    }

    // ---------- Registro ----------

    @Test
    void elRegistroCreaUnaCuentaPendiente() throws Exception {
        mvc.perform(json(post("/api/auth/registro"),
                        "{\"dni\":\"12345678\",\"nombres\":\"Luis\",\"apellidos\":\"Gomez\","
                                + "\"usuario\":\"luis.gomez\",\"clave\":\"clave-larga-123\",\"perfil\":\"CLIENTE\"}"))
                .andExpect(status().isCreated());
        Usuario creado = usuarios.findByUsuario("luis.gomez").orElseThrow();
        assertEquals(EstadoUsuario.PENDIENTE, creado.getEstado());
        assertTrue(encoder.matches("clave-larga-123", creado.getPassword()));
    }

    @Test
    void elRegistroRechazaDatosInvalidosYDuplicados() throws Exception {
        mvc.perform(json(post("/api/auth/registro"),
                        "{\"dni\":\"123\",\"nombres\":\"Luis\",\"apellidos\":\"Gomez\","
                                + "\"usuario\":\"luis.malo\",\"clave\":\"clave-larga-123\",\"perfil\":\"CLIENTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.dni").exists());

        crearUsuario("ya.existe", Perfil.CLIENTE, EstadoUsuario.ACTIVO);
        mvc.perform(json(post("/api/auth/registro"),
                        "{\"dni\":\"87654321\",\"nombres\":\"Luis\",\"apellidos\":\"Gomez\","
                                + "\"usuario\":\"ya.existe\",\"clave\":\"clave-larga-123\",\"perfil\":\"CLIENTE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.usuario").exists());
    }

    // ---------- Espacio del cliente ----------

    @Test
    void laSolicitudDeUnClienteConSesionQuedaEnSuEspacio() throws Exception {
        Usuario cliente = crearUsuario("cliente.mudanzas", Perfil.CLIENTE, EstadoUsuario.ACTIVO);
        String token = login("cliente.mudanzas", CLAVE, "");

        mvc.perform(json(post("/api/presupuestos"), solicitudJson("Mudanza Local"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/cliente/solicitudes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].usuarioId").value(cliente.getId()));
    }

    @Test
    void elClienteSoloCancelaSusSolicitudesPendientes() throws Exception {
        Usuario dueno = crearUsuario("cliente.dueno", Perfil.CLIENTE, EstadoUsuario.ACTIVO);
        crearUsuario("cliente.ajeno", Perfil.CLIENTE, EstadoUsuario.ACTIVO);
        Presupuesto propia = solicitud(dueno, EstadoPresupuesto.PENDIENTE);
        Presupuesto confirmada = solicitud(dueno, EstadoPresupuesto.CONFIRMADO);
        String tokenDueno = login("cliente.dueno", CLAVE, "");
        String tokenAjeno = login("cliente.ajeno", CLAVE, "");

        mvc.perform(post("/api/cliente/solicitudes/" + propia.getId() + "/cancelar")
                        .header("Authorization", "Bearer " + tokenAjeno))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/cliente/solicitudes/" + confirmada.getId() + "/cancelar")
                        .header("Authorization", "Bearer " + tokenDueno))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/cliente/solicitudes/" + propia.getId() + "/cancelar")
                        .header("Authorization", "Bearer " + tokenDueno))
                .andExpect(status().isNoContent());
        assertEquals(EstadoPresupuesto.CANCELADO, presupuestos.findById(propia.getId()).orElseThrow().getEstado());
    }

    @Test
    void elClienteCambiaSuClaveSabiendoLaActual() throws Exception {
        crearUsuario("cliente.clave", Perfil.CLIENTE, EstadoUsuario.ACTIVO);
        String token = login("cliente.clave", CLAVE, "");

        mvc.perform(json(post("/api/cliente/clave"),
                        "{\"actual\":\"equivocada\",\"nueva\":\"clave-nueva-456\",\"confirmacion\":\"clave-nueva-456\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.actual").exists());

        mvc.perform(json(post("/api/cliente/clave"),
                        "{\"actual\":\"" + CLAVE + "\",\"nueva\":\"clave-nueva-456\",\"confirmacion\":\"clave-nueva-456\"}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        login("cliente.clave", "clave-nueva-456", "");
    }

    // ---------- Administración ----------

    @Test
    void elAdministradorGestionaUsuariosPeroNoPuedeEliminarseASiMismo() throws Exception {
        String token = tokenAdmin("admin.gestor");
        Usuario otro = crearUsuario("cliente.gestionado", Perfil.CLIENTE, EstadoUsuario.PENDIENTE);
        Usuario yo = usuarios.findByUsuario("admin.gestor").orElseThrow();

        mvc.perform(get("/api/admin/usuarios").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("secretoTotp"))));

        mvc.perform(patch("/api/admin/usuarios/" + otro.getId() + "/estado?estado=ACTIVO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        assertEquals(EstadoUsuario.ACTIVO, usuarios.findById(otro.getId()).orElseThrow().getEstado());

        mvc.perform(delete("/api/admin/usuarios/" + yo.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/admin/usuarios/" + otro.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void elAdministradorCambiaElEstadoDeUnaSolicitud() throws Exception {
        String token = tokenAdmin("admin.solicitudes");
        Usuario dueno = crearUsuario("cliente.solicita", Perfil.CLIENTE, EstadoUsuario.ACTIVO);
        Presupuesto p = solicitud(dueno, EstadoPresupuesto.PENDIENTE);

        mvc.perform(patch("/api/admin/presupuestos/" + p.getId() + "/estado?estado=CONTACTADO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONTACTADO"));
    }

    private Presupuesto solicitud(Usuario dueno, EstadoPresupuesto estado) {
        Presupuesto p = new Presupuesto();
        p.setNombreCompleto("Marta Prueba");
        p.setEmail("marta@correo.com");
        p.setTelefono("600000000");
        p.setDireccionOrigen("Calle Origen 1");
        p.setDireccionDestino("Calle Destino 2");
        p.setTipoMudanza("Mudanza Local");
        p.setEstado(estado);
        p.setUsuarioId(dueno.getId());
        return presupuestos.save(p);
    }
}
