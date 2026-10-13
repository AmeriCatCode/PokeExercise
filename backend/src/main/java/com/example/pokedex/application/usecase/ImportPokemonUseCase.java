package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.LocalPokemonRepositoryPort;
import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.domain.catalog.IdOrName;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.exception.PokemonAlreadyImportedException;
import com.example.pokedex.domain.exception.PokemonNotFoundException;
import com.example.pokedex.domain.local.LocalPokemon;

/**
 * US-03: import a Pokémon from PokeAPI into the local database.
 * <p>
 * Deliberately not transactional: PokeAPI can be slow, and a transaction open during that call would
 * hold a database connection for nothing. The catalog is called first; only then does the repository
 * save, in its own short transaction.
 */
public class ImportPokemonUseCase {

	private final PokemonCatalogPort catalog;
	private final LocalPokemonRepositoryPort repository;

	public ImportPokemonUseCase(PokemonCatalogPort catalog, LocalPokemonRepositoryPort repository) {
		this.catalog = catalog;
		this.repository = repository;
	}

	/**
	 * @param idOrName a Pokémon number or name, in any case
	 * @throws PokemonNotFoundException        if PokeAPI has no such Pokémon
	 * @throws PokemonAlreadyImportedException if it is already in the local database
	 */
	public LocalPokemon importPokemon(String idOrName) {
		PokemonDetail detail = catalog.getPokemonDetail(IdOrName.requireValid(idOrName));
		// Friendly early check; the unique constraint of the database still decides when two imports
		// of the same Pokémon arrive at the same moment.
		if (repository.findIdByPokeapiId(detail.id()).isPresent()) {
			throw new PokemonAlreadyImportedException(detail.id());
		}
		return repository.add(LocalPokemon.importFrom(detail));
	}
}
