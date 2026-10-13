package com.example.pokedex.web.error;

import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.domain.exception.InvalidCredentialsException;
import com.example.pokedex.domain.exception.InvalidRequestException;
import com.example.pokedex.domain.exception.LocalPokemonNotFoundException;
import com.example.pokedex.domain.exception.PokemonAlreadyImportedException;
import com.example.pokedex.domain.exception.PokemonNotFoundException;
import com.example.pokedex.domain.exception.UserAlreadyExistsException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns every exception into the common {@link ErrorResponse} format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	private static final String INVALID_REQUEST = "Invalid request";

	/** A request parameter breaks a constraint, e.g. {@code @Min(0) int page}. */
	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException ex) {
		List<ErrorResponse.FieldError> fieldErrors = ex.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new ErrorResponse.FieldError(
								result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
				.toList();
		return build(HttpStatus.BAD_REQUEST, INVALID_REQUEST, fieldErrors);
	}

	/** A request parameter can't be converted, e.g. {@code page=abc}. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		String message = isWholeNumber(ex.getRequiredType()) ? "must be a whole number" : "has an invalid value";
		return build(HttpStatus.BAD_REQUEST, INVALID_REQUEST,
				List.of(new ErrorResponse.FieldError(ex.getName(), message)));
	}

	/** A request body breaks a constraint, e.g. {@code @NotBlank String username}. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
		List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		return build(HttpStatus.BAD_REQUEST, INVALID_REQUEST, fieldErrors);
	}

	/** The body is not valid JSON, or a value has the wrong type. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
		return build(HttpStatus.BAD_REQUEST, "Malformed request body", List.of());
	}

	@ExceptionHandler(InvalidRequestException.class)
	ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
		return build(HttpStatus.BAD_REQUEST, INVALID_REQUEST,
				List.of(new ErrorResponse.FieldError(ex.getField(), ex.getReason())));
	}

	@ExceptionHandler(UserAlreadyExistsException.class)
	ResponseEntity<ErrorResponse> handleUserAlreadyExists(UserAlreadyExistsException ex) {
		String message = "email".equals(ex.getField())
				? "This email is already registered"
				: "This username is already taken";
		return build(HttpStatus.CONFLICT, message, List.of(new ErrorResponse.FieldError(ex.getField(), message)));
	}

	@ExceptionHandler(InvalidCredentialsException.class)
	ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
		return build(HttpStatus.UNAUTHORIZED, "Invalid username or password", List.of());
	}

	@ExceptionHandler(PokemonNotFoundException.class)
	ResponseEntity<ErrorResponse> handlePokemonNotFound(PokemonNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, "Pokémon not found", List.of());
	}

	@ExceptionHandler(PokemonAlreadyImportedException.class)
	ResponseEntity<ErrorResponse> handleAlreadyImported(PokemonAlreadyImportedException ex) {
		return build(HttpStatus.CONFLICT, "This Pokémon is already imported", List.of());
	}

	@ExceptionHandler(LocalPokemonNotFoundException.class)
	ResponseEntity<ErrorResponse> handleLocalPokemonNotFound(LocalPokemonNotFoundException ex) {
		return build(HttpStatus.NOT_FOUND, "This Pokémon no longer exists in the local database", List.of());
	}

	@ExceptionHandler(CatalogUnavailableException.class)
	ResponseEntity<ErrorResponse> handleCatalogUnavailable(CatalogUnavailableException ex) {
		log.warn("PokeAPI unavailable: {}", ex.getMessage());
		return build(HttpStatus.BAD_GATEWAY, "PokeAPI is unavailable, please try again later", List.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		// Spring MVC's own exceptions (unknown route, unsupported method, ...) already know their
		// status: keep it and only change the body format.
		if (ex instanceof org.springframework.web.ErrorResponse springError) {
			HttpStatusCode status = springError.getStatusCode();
			return build(status, reasonPhrase(status), List.of());
		}
		log.error("Unexpected error", ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", List.of());
	}

	private static ResponseEntity<ErrorResponse> build(HttpStatusCode status, String message,
			List<ErrorResponse.FieldError> fieldErrors) {
		return ResponseEntity.status(status).body(ErrorResponse.of(status.value(), message, fieldErrors));
	}

	private static String reasonPhrase(HttpStatusCode status) {
		HttpStatus known = HttpStatus.resolve(status.value());
		return known != null ? known.getReasonPhrase() : "Error";
	}

	private static boolean isWholeNumber(Class<?> type) {
		return type == int.class || type == Integer.class || type == long.class || type == Long.class;
	}
}
