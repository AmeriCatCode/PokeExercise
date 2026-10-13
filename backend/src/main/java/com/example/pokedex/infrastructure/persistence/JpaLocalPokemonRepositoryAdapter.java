package com.example.pokedex.infrastructure.persistence;

import com.example.pokedex.application.port.LocalPokemonRepositoryPort;
import com.example.pokedex.domain.exception.LocalPokemonNotFoundException;
import com.example.pokedex.domain.exception.PokemonAlreadyImportedException;
import com.example.pokedex.domain.exception.VersionConflictException;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.local.ProprietaryFields;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.domain.shared.PageRequest;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link LocalPokemonRepositoryPort} on PostgreSQL. Each method runs in its own short transaction; none of
 * them ever calls PokeAPI. The unique constraint on {@code pokeapi_id} is the final judge of duplicates,
 * so two imports of the same Pokémon arriving at the same moment can never both succeed; the losing one
 * is reported as {@link PokemonAlreadyImportedException}, never as a database error.
 * <p>
 * Updates and deletes carry the version the user loaded (optimistic locking): the database only changes
 * the row if it still has that version, so a change made by someone else in between is never overwritten
 * or deleted unseen ({@link VersionConflictException}).
 */
@Component
class JpaLocalPokemonRepositoryAdapter implements LocalPokemonRepositoryPort {

	private static final String POKEAPI_ID_CONSTRAINT = "uk_pokemon_pokeapi_id";

	private final PokemonJpaRepository jpa;
	private final TransactionTemplate transaction;

	JpaLocalPokemonRepositoryAdapter(PokemonJpaRepository jpa, PlatformTransactionManager transactionManager) {
		this.jpa = jpa;
		this.transaction = new TransactionTemplate(transactionManager);
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
	public LocalPokemon update(long id, long expectedVersion, ProprietaryFields fields) {
		try {
			return transaction.execute(status -> {
				PokemonEntity entity = jpa.findById(id).orElseThrow(() -> new LocalPokemonNotFoundException(id));
				if (entity.version() != expectedVersion) {
					throw new VersionConflictException(id);
				}
				entity.changeProprietaryFields(fields);
				// The UPDATE says "where version = expectedVersion": if another update was saved since the
				// check above, it changes nothing and Hibernate reports an optimistic locking failure.
				return jpa.saveAndFlush(entity).toDomain();
			});
		}
		catch (OptimisticLockingFailureException ex) {
			throw notFoundOrConflict(id);
		}
	}

	@Override
	public void delete(long id, long expectedVersion) {
		Integer deleted = transaction.execute(status -> jpa.deleteByIdAndVersion(id, expectedVersion));
		if (deleted == null || deleted == 0) {
			throw notFoundOrConflict(id);
		}
	}

	/** Nothing was changed: either the Pokémon is gone, or it has another version by now. */
	private RuntimeException notFoundOrConflict(long id) {
		return jpa.existsById(id) ? new VersionConflictException(id) : new LocalPokemonNotFoundException(id);
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
