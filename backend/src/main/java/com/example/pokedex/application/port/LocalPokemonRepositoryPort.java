package com.example.pokedex.application.port;

import com.example.pokedex.domain.exception.LocalPokemonNotFoundException;
import com.example.pokedex.domain.exception.PokemonAlreadyImportedException;
import com.example.pokedex.domain.exception.VersionConflictException;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.local.ProprietaryFields;
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

	/**
	 * Saves new proprietary fields, only if the stored version is still the one the user loaded. Every
	 * successful update increases the version, also when only the tags change.
	 *
	 * @return the updated Pokémon, with its new version
	 * @throws LocalPokemonNotFoundException if there is no such Pokémon (e.g. another user deleted it)
	 * @throws VersionConflictException      if someone else changed it after {@code expectedVersion} was loaded,
	 *                                       including when two updates are saved at the same moment
	 */
	LocalPokemon update(long id, long expectedVersion, ProprietaryFields fields);

	/**
	 * Deletes the Pokémon, only if the stored version is still the one the user loaded.
	 *
	 * @throws LocalPokemonNotFoundException if there is no such Pokémon, e.g. another user already deleted it
	 * @throws VersionConflictException      if someone else changed it after {@code expectedVersion} was loaded
	 */
	void delete(long id, long expectedVersion);

	Optional<LocalPokemon> findById(long id);

	/** The local id of the Pokémon imported from this PokeAPI id, if any. Always read from the database. */
	Optional<Long> findIdByPokeapiId(int pokeapiId);

	/** The local Pokémon in PokeAPI number order. */
	Page<LocalPokemon> findAll(PageRequest request);
}
