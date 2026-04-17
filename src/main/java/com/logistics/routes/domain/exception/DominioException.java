package com.logistics.routes.domain.exception;

public abstract class DominioException extends RuntimeException {

    protected DominioException(String message) {
        super(message);
    }

    protected DominioException(String message, Throwable cause) {
        super(message, cause);
    }
}
