package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.LocalPokemonRepositoryPort;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.domain.shared.PageRequest;

/**
 * UC-06: browse the local Pokémon page by page.
 */
public class ListLocalPokemonUseCase {

	private final LocalPokemonRepositoryPort repository;

	public ListLocalPokemonUseCase(LocalPokemonRepositoryPort repository) {
		this.repository = repository;
	}

	public Page<LocalPokemon> list(int page, int size) {
		return repository.findAll(new PageRequest(page, size));
	}
}
