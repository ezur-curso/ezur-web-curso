package com.ezur.ezur_web.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/** Emite y valida tokens JWT firmados con HS256. El token lo guarda el frontend, nunca el navegador. */
@Service
public class JwtService {

    /** Lo que el token dice de quien lo presenta. */
    public record Sesion(String usuario, String perfil, boolean segundoFactor) {}

    private static final String ENCABEZADO = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODIFICADOR = Base64.getUrlDecoder();

    private final JsonMapper json = JsonMapper.builder().build();
    private final byte[] clave;
    private final long minutosDeVida;

    public JwtService(@Value("${app.jwt.secreto}") String secreto,
                      @Value("${app.jwt.minutos:120}") long minutosDeVida) {
        if (secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secreto debe tener al menos 32 caracteres");
        }
        this.clave = secreto.getBytes(StandardCharsets.UTF_8);
        this.minutosDeVida = minutosDeVida;
    }

    public String crear(String usuario, String perfil, boolean segundoFactor) {
        Map<String, Object> carga = new LinkedHashMap<>();
        carga.put("sub", usuario);
        carga.put("perfil", perfil);
        carga.put("fa", segundoFactor);
        carga.put("exp", Instant.now().plusSeconds(minutosDeVida * 60).getEpochSecond());

        String cuerpo = codificar(ENCABEZADO.getBytes(StandardCharsets.UTF_8))
                + "." + codificar(json.writeValueAsBytes(carga));
        return cuerpo + "." + codificar(firmar(cuerpo));
    }

    public Optional<Sesion> validar(String token) {
        try {
            String[] partes = token.split("\\.");
            if (partes.length != 3) {
                return Optional.empty();
            }
            byte[] esperada = firmar(partes[0] + "." + partes[1]);
            if (!MessageDigest.isEqual(esperada, DECODIFICADOR.decode(partes[2]))) {
                return Optional.empty();
            }
            Map<String, Object> carga = json.readValue(DECODIFICADOR.decode(partes[1]),
                    new TypeReference<Map<String, Object>>() {});
            long expira = ((Number) carga.get("exp")).longValue();
            if (Instant.now().getEpochSecond() >= expira) {
                return Optional.empty();
            }
            return Optional.of(new Sesion((String) carga.get("sub"), (String) carga.get("perfil"),
                    Boolean.TRUE.equals(carga.get("fa"))));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private byte[] firmar(String texto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave, "HmacSHA256"));
            return mac.doFinal(texto.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }

    private static String codificar(byte[] datos) {
        return CODIFICADOR.encodeToString(datos);
    }
}
