package com.example.taskapi.exception;

/** The JWT is valid but its subject does not match any known user. */
public class UnknownUserException extends RuntimeException {

    public UnknownUserException() {
        super("Authenticated user is not registered");
    }
}
