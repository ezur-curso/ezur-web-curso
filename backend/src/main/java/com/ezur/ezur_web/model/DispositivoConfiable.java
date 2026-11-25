package com.ezur.ezur_web.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dispositivos_confiables")
public class DispositivoConfiable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @ManyToOne(optional = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime expiraEn;

    protected DispositivoConfiable() {
    }

    public DispositivoConfiable(String tokenHash, Usuario usuario, LocalDateTime expiraEn) {
        this.tokenHash = tokenHash;
        this.usuario = usuario;
        this.expiraEn = expiraEn;
    }

    public Long getId() { return id; }
    public String getTokenHash() { return tokenHash; }
    public Usuario getUsuario() { return usuario; }
    public LocalDateTime getExpiraEn() { return expiraEn; }
}