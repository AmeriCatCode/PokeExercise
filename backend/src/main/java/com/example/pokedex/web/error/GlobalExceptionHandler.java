package com.example.pokedex.web.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns every exception into the common {@link ErrorResponse} format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		// Spring MVC's own exceptions (unknown route, unsupported method, ...) already know their
		// status: keep it and only change the body format.
		if (ex instanceof org.springframework.web.ErrorResponse springError) {
			HttpStatusCode status = springError.getStatusCode();
			return build(status, reasonPhrase(status));
		}
		log.error("Unexpected error", ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
	}

	private static ResponseEntity<ErrorResponse> build(HttpStatusCode status, String message) {
		return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), message));
	}

	private static String reasonPhrase(HttpStatusCode status) {
		HttpStatus known = HttpStatus.resolve(status.value());
		return known != null ? known.getReasonPhrase() : "Error";
	}
}
