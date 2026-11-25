package com.ezur.ezur_web.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ezur.ezur_web.model.PasswordResetToken;
import com.ezur.ezur_web.model.Usuario;


@Repository 
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    
    void deleteByUsuario(Usuario usuario);
} 
