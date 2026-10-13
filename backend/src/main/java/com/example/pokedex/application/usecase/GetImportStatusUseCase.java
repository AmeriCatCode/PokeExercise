package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.LocalPokemonRepositoryPort;
import java.util.Optional;

/**
 * Whether a catalog Pokémon is already imported, shown to logged-in users on the detail view.
 * <p>
 * Always a fresh database lookup, never cached with the PokeAPI data, so a Pokémon shows as imported
 * right after anyone imports it.
 */
public class GetImportStatusUseCase {

	private final LocalPokemonRepositoryPort repository;

	public GetImportStatusUseCase(LocalPokemonRepositoryPort repository) {
		this.repository = repository;
	}

	/** @return the local id, or empty when the Pokémon is not imported */
	public Optional<Long> localIdOf(int pokeapiId) {
		return repository.findIdByPokeapiId(pokeapiId);
	}
}
