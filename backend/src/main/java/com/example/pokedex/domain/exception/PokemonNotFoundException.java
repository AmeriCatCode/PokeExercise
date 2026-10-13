package com.example.pokedex.domain.exception;

/**
 * The requested Pokémon does not exist.
 */
public class PokemonNotFoundException extends RuntimeException {

	public PokemonNotFoundException(String idOrName) {
		super("Pokémon not found: " + idOrName);
	}
}
