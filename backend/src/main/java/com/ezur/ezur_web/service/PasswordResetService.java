package com.ezur.ezur_web.service;

import com.ezur.ezur_web.model.PasswordResetToken;
import com.ezur.ezur_web.model.Usuario;
import com.ezur.ezur_web.repository.PasswordResetTokenRepository;
import com.ezur.ezur_web.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@Service
public class PasswordResetService {

    private final DispositivoConfiableService dispositivoService;
    private static final int MINUTOS_DE_VALIDEZ = 30;

    private final SecureRandom random = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String baseUrl;

    public PasswordResetService(UsuarioRepository usuarioRepository,
                            PasswordResetTokenRepository tokenRepository,
                            EmailService emailService,
                            PasswordEncoder passwordEncoder,
                            DispositivoConfiableService dispositivoService) {
    this.usuarioRepository = usuarioRepository;
    this.tokenRepository = tokenRepository;
    this.emailService = emailService;
    this.passwordEncoder = passwordEncoder;
    this.dispositivoService = dispositivoService;
}

    @Transactional
    public void solicitarReset(String email) {
        usuarioRepository.findByEmail(email).ifPresent(usuario -> {
            tokenRepository.deleteByUsuario(usuario);

            String token = generarToken();
            tokenRepository.save(new PasswordResetToken(
                    hash(token), usuario, LocalDateTime.now().plusMinutes(MINUTOS_DE_VALIDEZ)));

            String enlace = baseUrl + "/restablecer-password?token=" + token;
            emailService.enviarHtml(usuario.getEmail(), "Restablecer tu contraseña - EZUR", "reset-password",
            Map.of("enlace", enlace, "minutos", MINUTOS_DE_VALIDEZ));
        });
    }

   
    @Transactional(readOnly = true)
    public boolean tokenValido(String token) {
        return buscarVigente(token).isPresent();
    }

    @Transactional
    public boolean cambiarPassword(String token, String passwordNueva) {
        Optional<PasswordResetToken> vigente = buscarVigente(token);
        if (vigente.isEmpty()) {
            return false;
        }

        Usuario usuario = vigente.get().getUsuario();
        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(usuario);

        tokenRepository.deleteByUsuario(usuario); // la llave queda gastada
        dispositivoService.olvidarTodos(usuario);
        return true;
    }

    private Optional<PasswordResetToken> buscarVigente(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenRepository.findByTokenHash(hash(token))
                .filter(t -> t.getExpiraEn().isAfter(LocalDateTime.now()));
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}