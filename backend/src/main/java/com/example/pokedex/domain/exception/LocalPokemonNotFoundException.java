package com.example.pokedex.domain.exception;

/**
 * There is no local Pokémon with this id, for example because another user deleted it.
 */
public class LocalPokemonNotFoundException extends RuntimeException {

	public LocalPokemonNotFoundException(long id) {
		super("Local Pokémon not found: " + id);
	}
}
