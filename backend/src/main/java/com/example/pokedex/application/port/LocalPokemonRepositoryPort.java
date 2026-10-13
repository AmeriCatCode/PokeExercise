package com.example.pokedex.application.port;

import com.example.pokedex.domain.exception.PokemonAlreadyImportedException;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.domain.shared.PageRequest;
import java.util.Optional;

/**
 * Storage of the Pokémon imported into the local database.
 */
public interface LocalPokemonRepositoryPort {

	/**
	 * Saves a newly imported Pokémon, in its own short transaction.
	 *
	 * @return the saved Pokémon, with its id, version and timestamps
	 * @throws PokemonAlreadyImportedException if a Pokémon with the same PokeAPI id is already stored,
	 *                                         including when another import of it is saved at the same moment
	 */
	LocalPokemon add(LocalPokemon pokemon);

	Optional<LocalPokemon> findById(long id);

	/** The local id of the Pokémon imported from this PokeAPI id, if any. Always read from the database. */
	Optional<Long> findIdByPokeapiId(int pokeapiId);

	/** The local Pokémon in PokeAPI number order. */
	Page<LocalPokemon> findAll(PageRequest request);
}
