package com.example.pokedex.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface PokemonJpaRepository extends JpaRepository<PokemonEntity, Long> {

	/** Only the id: the import indicator needs nothing else. */
	@Query("select p.id from PokemonEntity p where p.pokeapiId = :pokeapiId")
	Optional<Long> findIdByPokeapiId(int pokeapiId);

	/**
	 * Deletes the Pokémon only if it still has this version, in one statement, so a change saved at the
	 * same moment can't slip in between a check and the delete. Its child rows go with it.
	 *
	 * @return 1 if deleted, 0 if it doesn't exist or has another version
	 */
	@Modifying
	@Query("delete from PokemonEntity p where p.id = :id and p.version = :version")
	int deleteByIdAndVersion(long id, long version);
}
