package com.example.pokedex.web.error;

import java.time.Instant;
import java.util.List;

/**
 * The single error body returned by every endpoint of the API.
 */
public record ErrorResponse(int status, String message, List<FieldError> fieldErrors, Instant timestamp) {

	public record FieldError(String field, String message) {
	}

	public static ErrorResponse of(int status, String message) {
		return new ErrorResponse(status, message, List.of(), Instant.now());
	}

	public static ErrorResponse of(int status, String message, List<FieldError> fieldErrors) {
		return new ErrorResponse(status, message, List.copyOf(fieldErrors), Instant.now());
	}
}
