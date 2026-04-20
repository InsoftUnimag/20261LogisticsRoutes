package com.logistics.routes.domain.exception;

public class EmailDuplicadoException extends DominioException {

    public EmailDuplicadoException(String email) {
        super("Ya existe un conductor registrado con el email: " + email);
    }
}
