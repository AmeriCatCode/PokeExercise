package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.domain.catalog.IdOrName;
import com.example.pokedex.domain.catalog.PokemonDetail;

/**
 * US-02: see everything about one Pokémon.
 */
public class GetPokemonDetailUseCase {

	private final PokemonCatalogPort catalog;

	public GetPokemonDetailUseCase(PokemonCatalogPort catalog) {
		this.catalog = catalog;
	}

	public PokemonDetail get(String idOrName) {
		return catalog.getPokemonDetail(IdOrName.requireValid(idOrName));
	}
}
