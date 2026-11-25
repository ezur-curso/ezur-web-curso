package com.ezur.ezur_front.security;

import org.springframework.security.core.AuthenticationException;

/** Cuenta correcta, pero por el acceso equivocado (cliente en el de administración o al revés). */
public class AmbitoIncorrectoException extends AuthenticationException {

    public AmbitoIncorrectoException() {
        super("Cuenta correcta en el acceso equivocado");
    }
}
