package com.example.pokedex.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface PokemonJpaRepository extends JpaRepository<PokemonEntity, Long> {

	/** Only the id: the import indicator needs nothing else. */
	@Query("select p.id from PokemonEntity p where p.pokeapiId = :pokeapiId")
	Optional<Long> findIdByPokeapiId(int pokeapiId);
}
