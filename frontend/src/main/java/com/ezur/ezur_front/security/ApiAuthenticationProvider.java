package com.ezur.ezur_front.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ezur.ezur_front.api.ApiClient;
import com.ezur.ezur_front.api.ApiClient.LoginRespuesta;
import com.ezur.ezur_front.api.ApiException;

/**
 * Valida usuario y clave contra la API en lugar de la base de datos. Es un bean para que Spring Boot
 * no cree su usuario de ejemplo en memoria; el login lo usa a través de un ProviderManager propio.
 */
@Component
public class ApiAuthenticationProvider implements AuthenticationProvider {

    private final ApiClient api;

    public ApiAuthenticationProvider(ApiClient api) {
        this.api = api;
    }

    @Override
    public Authentication authenticate(Authentication intento) throws AuthenticationException {
        String clave = intento.getCredentials() == null ? "" : intento.getCredentials().toString();
        LoginRespuesta respuesta;
        try {
            respuesta = api.login(intento.getName(), clave, ambitoDeLaPeticion());
        } catch (ApiException e) {
            if ("inactivo".equals(e.getCodigo())) {
                throw new DisabledException("Cuenta sin activar", e);
            }
            if ("perfil".equals(e.getCodigo())) {
                throw new AmbitoIncorrectoException();
            }
            throw new BadCredentialsException("Credenciales incorrectas", e);
        } catch (RestClientException e) {
            throw new AuthenticationServiceException("No se pudo contactar con la API", e);
        }
        UsuarioApi usuario = new UsuarioApi(respuesta.usuario(), respuesta.perfil(), respuesta.token());
        return UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> tipo) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
    }

    // El formulario de cada acceso manda "ambito": la API decide si la cuenta pertenece a ese acceso
    private static String ambitoDeLaPeticion() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest().getParameter("ambito");
        }
        return null;
    }
}
