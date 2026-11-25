package com.ezur.ezur_web.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity 
@Table (name = "password_reset_tokens")
public class PasswordResetToken {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column (nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime expiraEn;

    protected PasswordResetToken() {
    }

    public PasswordResetToken(String tokenHash, Usuario usuario, LocalDateTime expiraEn) {
        this.tokenHash = tokenHash;
        this.usuario = usuario;
        this.expiraEn = expiraEn;
    }

    public long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getExpiraEn() {
        return expiraEn;
    }

}
