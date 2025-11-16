package com.projet.adhesionapp.common.exception;


/**
 * Représente une erreur 400 - requête invalide.
 */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(message);
    }
}

