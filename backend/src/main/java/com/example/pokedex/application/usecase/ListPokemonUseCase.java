package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.domain.catalog.PokemonSummary;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.domain.shared.PageRequest;

/**
 * US-01: browse the Pokémon catalog page by page.
 */
public class ListPokemonUseCase {

	private final PokemonCatalogPort catalog;

	public ListPokemonUseCase(PokemonCatalogPort catalog) {
		this.catalog = catalog;
	}

	public Page<PokemonSummary> list(int page, int size) {
		return catalog.listPokemon(new PageRequest(page, size));
	}
}
