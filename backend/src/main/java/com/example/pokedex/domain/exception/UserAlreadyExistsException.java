package com.example.pokedex.domain.exception;

/**
 * The username or the email is already used by another account.
 */
public class UserAlreadyExistsException extends RuntimeException {

	private final String field;

	/**
	 * @param field "username" or "email"
	 */
	public UserAlreadyExistsException(String field) {
		super("A user with this " + field + " already exists");
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
