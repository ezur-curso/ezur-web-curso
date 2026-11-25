package com.ezur.ezur_web.dto;

import com.ezur.ezur_web.model.Perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegistroRequest {

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos")
    private String dni;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres son demasiado largos")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos son demasiado largos")
    private String apellidos;

    @NotBlank(message = "El usuario es obligatorio")
    @Pattern(regexp = "[A-Za-z0-9._-]{4,30}",
             message = "El usuario debe tener entre 4 y 30 caracteres (letras, números, punto, guion)")
    private String usuario;

    @NotBlank(message = "La clave es obligatoria")
    @Size(min = 10, max = 72, message = "La clave debe tener entre 10 y 72 caracteres")
    private String clave;

    @NotNull(message = "Elige un perfil")
    private Perfil perfil = Perfil.CLIENTE;

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
}
