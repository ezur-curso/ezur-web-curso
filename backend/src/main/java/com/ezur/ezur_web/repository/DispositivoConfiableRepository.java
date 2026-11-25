package com.ezur.ezur_web.repository;

import com.ezur.ezur_web.model.DispositivoConfiable;
import com.ezur.ezur_web.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DispositivoConfiableRepository extends JpaRepository<DispositivoConfiable, Long> {

    Optional<DispositivoConfiable> findByTokenHash(String tokenHash);

    void deleteByUsuario(Usuario usuario);

    void deleteByExpiraEnBefore(LocalDateTime momento);
}