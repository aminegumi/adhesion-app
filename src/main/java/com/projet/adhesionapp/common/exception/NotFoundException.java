package com.projet.adhesionapp.common.exception;

/**
 * Représente une erreur 404 - ressource non trouvée.
 */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(message);
    }
}

