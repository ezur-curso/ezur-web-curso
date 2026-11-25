package com.ezur.ezur_web.service;

import com.ezur.ezur_web.model.DispositivoConfiable;
import com.ezur.ezur_web.model.Usuario;
import com.ezur.ezur_web.repository.DispositivoConfiableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DispositivoConfiableService {

    public static final int DIAS_DE_VALIDEZ = 30;

    private final DispositivoConfiableRepository repository;

    public DispositivoConfiableService(DispositivoConfiableRepository repository) {
        this.repository = repository;
    }

    // Registra un dispositivo nuevo y devuelve la llave que va en la cookie
    @Transactional
    public String recordar(Usuario usuario) {
        repository.deleteByExpiraEnBefore(LocalDateTime.now()); 

        String token = TokenUtil.generar();
        repository.save(new DispositivoConfiable(
                TokenUtil.hash(token), usuario, LocalDateTime.now().plusDays(DIAS_DE_VALIDEZ)));
        return token;
    }

    @Transactional(readOnly = true)
    public boolean esConfiable(Usuario usuario, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return repository.findByTokenHash(TokenUtil.hash(token))
                .filter(d -> d.getUsuario().getId().equals(usuario.getId()))
                .filter(d -> d.getExpiraEn().isAfter(LocalDateTime.now()))
                .isPresent();
    }

    // Para cuando cambia la contraseña, todos los dispositivos vuelven a pedir código
    @Transactional
    public void olvidarTodos(Usuario usuario) {
        repository.deleteByUsuario(usuario);
    }
}
