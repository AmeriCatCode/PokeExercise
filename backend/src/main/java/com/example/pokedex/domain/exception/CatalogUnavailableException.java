package com.example.pokedex.domain.exception;

/**
 * The external Pokémon catalog (PokeAPI) is down, too slow or returned an unusable answer.
 */
public class CatalogUnavailableException extends RuntimeException {

	public CatalogUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

	public CatalogUnavailableException(String message) {
		super(message);
	}
}
