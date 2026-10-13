package com.example.pokedex.domain.exception;

/**
 * Wrong username or password. Deliberately doesn't say which one, so it doesn't reveal which usernames exist.
 */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException() {
		super("Invalid username or password");
	}
}
