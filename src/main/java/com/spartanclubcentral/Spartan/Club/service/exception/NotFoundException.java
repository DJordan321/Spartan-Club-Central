package com.spartanclubcentral.Spartan.Club.service.exception;

/**
 * Thrown by services when something a user asked for does not exist or is
 * not public (for example an inactive club). GlobalExceptionHandler turns it
 * into the friendly 404 page, so users never see technical details (SPR-06).
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
