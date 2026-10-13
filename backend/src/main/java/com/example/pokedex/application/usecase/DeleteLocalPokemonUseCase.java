package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.LocalPokemonRepositoryPort;
import com.example.pokedex.domain.exception.LocalPokemonNotFoundException;
import com.example.pokedex.domain.exception.VersionConflictException;

/**
 * US-04: delete a local Pokémon. Like an update, it needs the version the user loaded, so nobody deletes
 * a change they never saw.
 */
public class DeleteLocalPokemonUseCase {

	private final LocalPokemonRepositoryPort repository;

	public DeleteLocalPokemonUseCase(LocalPokemonRepositoryPort repository) {
		this.repository = repository;
	}

	/**
	 * @throws LocalPokemonNotFoundException if the Pokémon no longer exists (e.g. two users deleted it)
	 * @throws VersionConflictException      if someone else changed it after it was loaded
	 */
	public void delete(long id, long version) {
		repository.delete(id, version);
	}
}
