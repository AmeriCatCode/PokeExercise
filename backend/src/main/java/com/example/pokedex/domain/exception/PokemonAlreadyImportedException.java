package com.example.pokedex.domain.exception;

/**
 * The Pokémon is already in the local database: each PokeAPI Pokémon can be imported only once.
 */
public class PokemonAlreadyImportedException extends RuntimeException {

	private final int pokeapiId;

	public PokemonAlreadyImportedException(int pokeapiId) {
		super("Pokémon already imported: " + pokeapiId);
		this.pokeapiId = pokeapiId;
	}

	public int getPokeapiId() {
		return pokeapiId;
	}
}
