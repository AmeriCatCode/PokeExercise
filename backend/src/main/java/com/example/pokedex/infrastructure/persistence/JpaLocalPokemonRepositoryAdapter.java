package com.example.pokedex.infrastructure.persistence;

import com.example.pokedex.application.port.LocalPokemonRepositoryPort;
import com.example.pokedex.domain.exception.PokemonAlreadyImportedException;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.domain.shared.PageRequest;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link LocalPokemonRepositoryPort} on PostgreSQL. Each method runs in its own short transaction; none of
 * them ever calls PokeAPI. The unique constraint on {@code pokeapi_id} is the final judge of duplicates,
 * so two imports of the same Pokémon arriving at the same moment can never both succeed; the losing one
 * is reported as {@link PokemonAlreadyImportedException}, never as a database error.
 */
@Component
class JpaLocalPokemonRepositoryAdapter implements LocalPokemonRepositoryPort {

	private static final String POKEAPI_ID_CONSTRAINT = "uk_pokemon_pokeapi_id";

	private final PokemonJpaRepository jpa;

	JpaLocalPokemonRepositoryAdapter(PokemonJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	@Transactional
	public LocalPokemon add(LocalPokemon pokemon) {
		try {
			return jpa.saveAndFlush(PokemonEntity.newImport(pokemon)).toDomain();
		}
		catch (DataIntegrityViolationException ex) {
			throw translate(ex, pokemon.pokeapiId());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<LocalPokemon> findById(long id) {
		return jpa.findById(id).map(PokemonEntity::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Long> findIdByPokeapiId(int pokeapiId) {
		return jpa.findIdByPokeapiId(pokeapiId);
	}

	@Override
	@Transactional(readOnly = true)
	public Page<LocalPokemon> findAll(PageRequest request) {
		var page = jpa.findAll(org.springframework.data.domain.PageRequest.of(request.page(), request.size(),
				Sort.by("pokeapiId")));
		return new Page<>(page.getContent().stream().map(PokemonEntity::toDomain).toList(), request.page(),
				request.size(), page.getTotalElements());
	}

	private static RuntimeException translate(DataIntegrityViolationException ex, int pokeapiId) {
		String constraint = ex.getCause() instanceof ConstraintViolationException violation
				? violation.getConstraintName()
				: null;
		if (constraint != null && constraint.toLowerCase().contains(POKEAPI_ID_CONSTRAINT)) {
			return new PokemonAlreadyImportedException(pokeapiId);
		}
		return ex;
	}
}
