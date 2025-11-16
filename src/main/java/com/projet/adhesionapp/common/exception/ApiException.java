package com.projet.adhesionapp.common.exception;

/**
 * Exception de base pour les erreurs métier de l'API.
 */
public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
    }
}

