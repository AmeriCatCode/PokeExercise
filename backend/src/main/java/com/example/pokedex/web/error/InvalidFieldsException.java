package com.example.pokedex.web.error;

import java.util.List;

/**
 * A request body has fields it must not have (read-only or unknown ones): answered with {@code 400},
 * listing each of them.
 */
public class InvalidFieldsException extends RuntimeException {

	private final List<ErrorResponse.FieldError> fieldErrors;

	public InvalidFieldsException(List<ErrorResponse.FieldError> fieldErrors) {
		super("Invalid fields: " + fieldErrors);
		this.fieldErrors = List.copyOf(fieldErrors);
	}

	public List<ErrorResponse.FieldError> getFieldErrors() {
		return fieldErrors;
	}
}
