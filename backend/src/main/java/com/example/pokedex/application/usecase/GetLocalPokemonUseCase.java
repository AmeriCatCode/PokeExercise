package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.LocalPokemonRepositoryPort;
import com.example.pokedex.domain.exception.LocalPokemonNotFoundException;
import com.example.pokedex.domain.local.LocalPokemon;

/**
 * UC-06: view one local Pokémon.
 */
public class GetLocalPokemonUseCase {

	private final LocalPokemonRepositoryPort repository;

	public GetLocalPokemonUseCase(LocalPokemonRepositoryPort repository) {
		this.repository = repository;
	}

	/**
	 * @throws LocalPokemonNotFoundException if there is no such local Pokémon, e.g. another user deleted it
	 */
	public LocalPokemon get(long id) {
		return repository.findById(id).orElseThrow(() -> new LocalPokemonNotFoundException(id));
	}
}
