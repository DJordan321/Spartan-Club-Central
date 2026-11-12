package com.spartanclubcentral.Spartan.Club.controller;

import com.spartanclubcentral.Spartan.Club.service.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Controller layer: turns errors into friendly pages (SPR-06).
 *
 * Users only ever see "Page not found" or "Something went wrong": never a
 * stack trace, SQL or class name. Details go to the server log instead.
 * A leader trying to use another leader's club gets the 403 page (SPR-03).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound() {
        return "error/404";
    }

    /** /clubs/abc or ?category=abc: treat a malformed address like a missing page. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String badParameter(MethodArgumentTypeMismatchException e) {
        log.debug("Bad value for parameter '{}'", e.getName());
        return "error/404";
    }

    /** A leader acting on a club they don't own (SPR-03). ClubService already logged OWNERSHIP_DENIED. */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String forbidden() {
        return "error/403";
    }

    /** An upload far over the size limit (the 2 MB rule itself is checked in ClubImageService). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String tooLarge() {
        return "error/413";
    }
}
