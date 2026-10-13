package com.example.pokedex.application.port;

import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.PokemonSummary;
import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.domain.exception.PokemonNotFoundException;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.domain.shared.PageRequest;

/**
 * The external Pokémon catalog (implemented by the PokeAPI adapter).
 */
public interface PokemonCatalogPort {

	/**
	 * @throws CatalogUnavailableException if the catalog fails, also for a single Pokémon of the page
	 */
	Page<PokemonSummary> listPokemon(PageRequest request);

	/**
	 * @param idOrName a Pokémon number or name, in any case
	 * @throws PokemonNotFoundException    if the catalog has no such Pokémon
	 * @throws CatalogUnavailableException if the catalog fails
	 */
	PokemonDetail getPokemonDetail(String idOrName);
}
