package com.ezur.ezur_front;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ezur.ezur_front.api.ApiClient;
import com.ezur.ezur_front.api.ApiException;
import com.ezur.ezur_front.model.Testimonio;
import com.ezur.ezur_front.model.TipoServicio;

/** El frontend no tiene base de datos: la API se sustituye por un doble y se comprueban las páginas. */
@SpringBootTest
@AutoConfigureMockMvc
class PaginasPublicasTests {

    private static final String[] PAGINAS = {
        "/", "/servicios", "/nosotros", "/contacto", "/cotizar",
        "/politica-privacidad", "/login", "/registro", "/admin-login", "/olvide-password"
    };

    // Solo pueden aparecer datos de ejemplo: el único correo de contacto es el de ejemplo
    // y el único teléfono de 9 cifras es el de ejemplo (900 000 000).
    private static final Pattern CORREO = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
    private static final Pattern TELEFONO = Pattern.compile("(?<![\\d.#])(?!900)\\d{3}[ .-]?\\d{3}[ .-]?\\d{3}(?![\\d%])");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ApiClient api;

    @BeforeEach
    void apiConDatos() {
        Testimonio opinion = new Testimonio();
        opinion.setNombre("Ana");
        opinion.setTexto("Todo llegó perfecto");
        opinion.setServicio("Mudanza Local");
        opinion.setEstrellas("5");
        when(api.testimonios()).thenReturn(List.of(opinion));
        when(api.tiposServicio()).thenReturn(List.of(
                new TipoServicio("Mudanza Local"), new TipoServicio("Oficina / Comercio")));
    }

    @Test
    void todasLasPaginasPublicasCargan() throws Exception {
        for (String pagina : PAGINAS) {
            mvc.perform(get(pagina)).andExpect(status().isOk());
        }
    }

    @Test
    void ningunaPaginaMuestraDatosReales() throws Exception {
        for (String pagina : PAGINAS) {
            String html = mvc.perform(get(pagina)).andReturn().getResponse().getContentAsString();
            Matcher correo = CORREO.matcher(html);
            while (correo.find()) {
                String direccion = correo.group();
                assertTrue(direccion.endsWith("@ezur.example") || direccion.equals("ejemplo@correo.com"),
                        pagina + " muestra un correo que no es de ejemplo");
            }
            assertFalse(TELEFONO.matcher(html).find(), pagina + " muestra un teléfono que no es de ejemplo");
        }
    }

    @Test
    void elPieMuestraLosDatosDeEjemplo() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("+34 900 000 000")))
                .andExpect(content().string(containsString("hola@ezur.example")));
    }

    @Test
    void laPortadaMuestraLasOpinionesDeLaApi() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("Todo llegó perfecto")))
                .andExpect(content().string(containsString("5,0")));
    }

    @Test
    void laPortadaTraeLaCalculadoraDeMudanza() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(containsString("data-calculadora")))
                .andExpect(content().string(containsString("Oficina / Comercio")));
    }

    @Test
    void cotizarRecibeElDetalleDeLaCalculadora() throws Exception {
        mvc.perform(get("/cotizar").param("detalle", "Piso de 3 habitaciones, estimacion de prueba"))
                .andExpect(content().string(containsString("Piso de 3 habitaciones, estimacion de prueba")));
    }

    @Test
    void cotizarSeRellenaDesdeLaPortada() throws Exception {
        mvc.perform(get("/cotizar").param("origen", "Calle Prueba 1").param("destino", "Av. Prueba 2"))
                .andExpect(content().string(containsString("Calle Prueba 1")))
                .andExpect(content().string(containsString("Av. Prueba 2")));
    }

    @Test
    void laSolicitudDePresupuestoSeEnviaALaApi() throws Exception {
        mvc.perform(post("/cotizar").with(csrf())
                        .param("nombreCompleto", "Marta Prueba").param("email", "marta@correo.com")
                        .param("telefono", "600000000").param("direccionOrigen", "Calle Origen 1")
                        .param("direccionDestino", "Calle Destino 2").param("tipoMudanza", "Mudanza Local")
                        .param("aceptaPrivacidad", "true"))
                .andExpect(redirectedUrl("/cotizar"));
        verify(api).solicitarPresupuesto(any());
    }

    @Test
    void siLaApiRechazaUnCampoSeMuestraSuMensajeEnElFormulario() throws Exception {
        when(api.solicitarPresupuesto(any())).thenThrow(new ApiException(400, "Datos inválidos", null,
                Map.of("tipoMudanza", "Elige un tipo de mudanza de la lista")));
        mvc.perform(post("/cotizar").with(csrf())
                        .param("nombreCompleto", "Marta Prueba").param("email", "marta@correo.com")
                        .param("telefono", "600000000").param("direccionOrigen", "Calle Origen 1")
                        .param("direccionDestino", "Calle Destino 2").param("tipoMudanza", "Otro")
                        .param("aceptaPrivacidad", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Elige un tipo de mudanza de la lista")));
    }

    @Test
    void sinLaApiLasPaginasQueLaNecesitanAvisanSinCaerse() throws Exception {
        when(api.testimonios()).thenThrow(new org.springframework.web.client.ResourceAccessException("sin conexión"));
        mvc.perform(get("/")).andExpect(status().isServiceUnavailable());
        // Las páginas que no usan datos siguen funcionando
        mvc.perform(get("/servicios")).andExpect(status().isOk());
    }
}
