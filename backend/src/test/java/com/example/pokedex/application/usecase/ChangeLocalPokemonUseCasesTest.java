package com.example.pokedex.application.usecase;

import static com.example.pokedex.application.usecase.FakeLocalPokemonRepository.detailWithId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.domain.exception.InvalidRequestException;
import com.example.pokedex.domain.exception.LocalPokemonNotFoundException;
import com.example.pokedex.domain.exception.VersionConflictException;
import com.example.pokedex.domain.local.LocalPokemon;
import java.util.List;
import org.junit.jupiter.api.Test;

/** US-04: update and delete a local Pokémon, both with the version the user loaded. */
class ChangeLocalPokemonUseCasesTest {

	private final FakeLocalPokemonRepository repository = new FakeLocalPokemonRepository();
	private final UpdateLocalPokemonUseCase update = new UpdateLocalPokemonUseCase(repository);
	private final DeleteLocalPokemonUseCase delete = new DeleteLocalPokemonUseCase(repository);
	private final LocalPokemon pikachu = repository.add(LocalPokemon.importFrom(detailWithId(25)));

	// --- update ------------------------------------------------------------------------------------

	@Test
	void updatesTheProprietaryFieldsAndReturnsTheNewVersion() {
		LocalPokemon updated = update.update(pikachu.id(), 0, "Pikachu de Ash", "Kanto", List.of("starter"));

		assertThat(updated.localName()).isEqualTo("Pikachu de Ash");
		assertThat(updated.localRegion()).isEqualTo("Kanto");
		assertThat(updated.tags()).containsExactly("starter");
		assertThat(updated.version()).isEqualTo(1);
		assertThat(updated.pokeApiData()).isEqualTo(pikachu.pokeApiData());
		assertThat(repository.findById(pikachu.id())).contains(updated);
	}

	@Test
	void updatingAMissingPokemonIsNotFound() {
		// For example, another user deleted it.
		assertThatThrownBy(() -> update.update(99, 0, "Name", null, List.of()))
				.isInstanceOf(LocalPokemonNotFoundException.class);
	}

	@Test
	void updatingWithAVersionThatDoesNotMatchIsAConflict() {
		// Ana saves first; Ben still has version 0.
		update.update(pikachu.id(), 0, "Ana's name", null, List.of());

		assertThatThrownBy(() -> update.update(pikachu.id(), 0, "Ben's name", null, List.of()))
				.isInstanceOf(VersionConflictException.class);
		assertThat(repository.findById(pikachu.id()).orElseThrow().localName()).isEqualTo("Ana's name");
	}

	@Test
	void invalidValuesAreRejectedWithoutSaving() {
		assertThatThrownBy(() -> update.update(pikachu.id(), 0, "x".repeat(101), null, List.of()))
				.isInstanceOf(InvalidRequestException.class);
		assertThat(repository.findById(pikachu.id())).contains(pikachu);
	}

	// --- delete ------------------------------------------------------------------------------------

	@Test
	void deletesThePokemon() {
		delete.delete(pikachu.id(), 0);

		assertThat(repository.findById(pikachu.id())).isEmpty();
	}

	@Test
	void deletingWithAVersionThatDoesNotMatchIsAConflict() {
		// Ana edits; Ben, who never saw that change, tries to delete version 0.
		update.update(pikachu.id(), 0, "Ana's name", null, List.of());

		assertThatThrownBy(() -> delete.delete(pikachu.id(), 0)).isInstanceOf(VersionConflictException.class);
		assertThat(repository.findById(pikachu.id())).isPresent();
	}

	@Test
	void deletingTwiceIsNotFoundTheSecondTime() {
		delete.delete(pikachu.id(), 0);

		assertThatThrownBy(() -> delete.delete(pikachu.id(), 0)).isInstanceOf(LocalPokemonNotFoundException.class);
	}
}
