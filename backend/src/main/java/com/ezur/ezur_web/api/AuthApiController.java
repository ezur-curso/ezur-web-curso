package com.ezur.ezur_web.api;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ezur.ezur_web.dto.RegistroRequest;
import com.ezur.ezur_web.model.Perfil;
import com.ezur.ezur_web.model.Usuario;
import com.ezur.ezur_web.repository.UsuarioRepository;
import com.ezur.ezur_web.security.JwtService;
import com.ezur.ezur_web.service.DispositivoConfiableService;
import com.ezur.ezur_web.service.PasswordResetService;
import com.ezur.ezur_web.service.TotpService;
import com.ezur.ezur_web.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Acceso: login, registro, 2FA del administrador y recuperación de clave. */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private static final Logger log = LoggerFactory.getLogger(AuthApiController.class);
    private static final String AMBITO_ADMIN = "admin";
    private static final int LARGO_MINIMO_CLAVE = 10;
    private static final int BYTES_MAXIMOS_CLAVE = 72; // límite de BCrypt

    public record LoginRequest(String usuario, String clave, String ambito) {}
    public record LoginRespuesta(String token, String usuario, String perfil, boolean requiere2fa) {}
    public record CodigoRequest(String codigo, Boolean recordar, String secreto) {}
    public record EmailRequest(String email) {}
    public record RestablecerRequest(String token, String clave) {}

    private final AuthenticationManager autenticador;
    private final JwtService jwt;
    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final TotpService totpService;
    private final DispositivoConfiableService dispositivoService;
    private final PasswordResetService resetService;

    public AuthApiController(AuthenticationManager autenticador, JwtService jwt, UsuarioService usuarioService,
                             UsuarioRepository usuarioRepository, TotpService totpService,
                             DispositivoConfiableService dispositivoService, PasswordResetService resetService) {
        this.autenticador = autenticador;
        this.jwt = jwt;
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.totpService = totpService;
        this.dispositivoService = dispositivoService;
        this.resetService = resetService;
    }

    // ---------- Login ----------

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest peticion, HttpServletRequest request) {
        if (peticion.usuario() == null || peticion.clave() == null) {
            return respuesta(HttpStatus.UNAUTHORIZED, "credenciales");
        }
        var intento = UsernamePasswordAuthenticationToken.unauthenticated(peticion.usuario(), peticion.clave());
        intento.setDetails(new WebAuthenticationDetails(ipDelCliente(request), null));

        Authentication auth;
        try {
            auth = autenticador.authenticate(intento);
        } catch (DisabledException e) {
            return respuesta(HttpStatus.FORBIDDEN, "inactivo");
        } catch (BadCredentialsException e) {
            return respuesta(HttpStatus.UNAUTHORIZED, "credenciales");
        }

        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        // Hay dos accesos independientes: una cuenta correcta en el acceso equivocado no entra
        if (admin != AMBITO_ADMIN.equals(peticion.ambito())) {
            return respuesta(HttpStatus.FORBIDDEN, "perfil");
        }
        String perfil = admin ? Perfil.ADMINISTRADOR.name() : Perfil.CLIENTE.name();
        // El administrador recibe un token "a medias" que solo sirve para el 2FA
        return ResponseEntity.ok(new LoginRespuesta(jwt.crear(auth.getName(), perfil, !admin), auth.getName(),
                perfil, admin));
    }

    // ---------- Registro ----------

    @PostMapping("/registro")
    public ResponseEntity<Void> registrar(@Valid @RequestBody RegistroRequest request) {
        var result = new BeanPropertyBindingResult(request, "registro");
        usuarioService.validarRegistro(request, result);
        ValidacionException.siHayErrores(result);
        try {
            usuarioService.registrar(request);
        } catch (IllegalStateException e) {
            throw new ValidacionException("usuario", "El DNI o el usuario ya están registrados");
        }
        log.info("Registro nuevo (pendiente de activación): usuario={}", request.getUsuario());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ---------- 2FA del administrador ----------

    /** Estado del segundo factor: OK (dispositivo de confianza), VERIFICAR o CONFIGURAR (primera vez). */
    @GetMapping("/2fa")
    public Map<String, Object> estado2fa(Authentication auth,
                                         @RequestHeader(value = "X-Dispositivo", required = false) String dispositivo,
                                         @RequestParam(required = false) String secreto) {
        Usuario usuario = usuarioRepository.findByUsuario(auth.getName()).orElseThrow();
        Map<String, Object> cuerpo = new HashMap<>();
        if (usuario.getSecretoTotp() != null) {
            if (dispositivoService.esConfiable(usuario, dispositivo)) {
                cuerpo.put("estado", "OK");
                cuerpo.put("token", jwt.crear(usuario.getUsuario(), usuario.getPerfil().name(), true));
            } else {
                cuerpo.put("estado", "VERIFICAR");
            }
            return cuerpo;
        }
        // Primera vez: el secreto no se guarda hasta que el código demuestre que el móvil lo tiene
        String nuevo = secreto != null && !secreto.isBlank() ? secreto : totpService.generarSecreto();
        cuerpo.put("estado", "CONFIGURAR");
        cuerpo.put("secreto", nuevo);
        cuerpo.put("qr", totpService.generarQrDataUri(nuevo, usuario.getUsuario()));
        return cuerpo;
    }

    @PostMapping("/2fa")
    public ResponseEntity<Map<String, Object>> verificar2fa(@RequestBody CodigoRequest peticion,
                                                            Authentication auth, HttpServletRequest request) {
        Usuario usuario = usuarioRepository.findByUsuario(auth.getName()).orElseThrow();
        boolean configurando = usuario.getSecretoTotp() == null;
        String secreto = configurando ? peticion.secreto() : usuario.getSecretoTotp();

        if (secreto == null || peticion.codigo() == null || !totpService.codigoValido(secreto, peticion.codigo())) {
            log.warn("2FA fallido: usuario={} ip={}", usuario.getUsuario(), ipDelCliente(request));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "Código incorrecto"));
        }
        if (configurando) {
            usuario.setSecretoTotp(secreto);
            usuarioRepository.save(usuario);
        }
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("token", jwt.crear(usuario.getUsuario(), usuario.getPerfil().name(), true));
        if (Boolean.TRUE.equals(peticion.recordar())) {
            cuerpo.put("dispositivo", dispositivoService.recordar(usuario));
        }
        return ResponseEntity.ok(cuerpo);
    }

    // ---------- Recuperación de clave ----------

    @PostMapping("/olvide")
    public ResponseEntity<Void> olvide(@RequestBody EmailRequest peticion) {
        try {
            if (peticion.email() != null) {
                resetService.solicitarReset(peticion.email().trim());
            }
        } catch (Exception e) {
            // Siempre se responde igual para no revelar qué correos existen
            log.error("No se pudo procesar la solicitud de restablecimiento", e);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/restablecer/{token}")
    public Map<String, Boolean> tokenValido(@PathVariable String token) {
        return Map.of("valido", resetService.tokenValido(token));
    }

    @PostMapping("/restablecer")
    public ResponseEntity<Map<String, Object>> restablecer(@RequestBody RestablecerRequest peticion) {
        String clave = peticion.clave() == null ? "" : peticion.clave();
        if (clave.length() < LARGO_MINIMO_CLAVE) {
            throw new ValidacionException("clave",
                    "La contraseña debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres.");
        }
        if (clave.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS_CLAVE) {
            throw new ValidacionException("clave", "La contraseña es demasiado larga.");
        }
        if (!resetService.cambiarPassword(peticion.token(), clave)) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("mensaje", "El enlace ya no es válido"));
        }
        return ResponseEntity.noContent().build();
    }

    private static ResponseEntity<Map<String, String>> respuesta(HttpStatus estado, String codigo) {
        return ResponseEntity.status(estado).body(Map.of("codigo", codigo));
    }

    static String ipDelCliente(HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        return reenviada != null && !reenviada.isBlank() ? reenviada.split(",")[0].trim() : request.getRemoteAddr();
    }
}
