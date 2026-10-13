package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.LocalPokemonRepositoryPort;
import com.example.pokedex.domain.exception.InvalidRequestException;
import com.example.pokedex.domain.exception.LocalPokemonNotFoundException;
import com.example.pokedex.domain.exception.VersionConflictException;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.local.ProprietaryFields;
import java.util.List;

/**
 * US-04: change the proprietary fields of a local Pokémon. The PokeAPI data can't be changed: PokeAPI is
 * its source of truth.
 */
public class UpdateLocalPokemonUseCase {

	private final LocalPokemonRepositoryPort repository;

	public UpdateLocalPokemonUseCase(LocalPokemonRepositoryPort repository) {
		this.repository = repository;
	}

	/**
	 * @param version the version the user loaded: the update is rejected if someone changed it since
	 * @return the updated Pokémon, with its new version
	 * @throws InvalidRequestException       if a value breaks a rule
	 * @throws LocalPokemonNotFoundException if the Pokémon no longer exists
	 * @throws VersionConflictException      if someone else changed it after it was loaded
	 */
	public LocalPokemon update(long id, long version, String localName, String localRegion, List<String> tags) {
		return repository.update(id, version, new ProprietaryFields(localName, localRegion, tags));
	}
}
