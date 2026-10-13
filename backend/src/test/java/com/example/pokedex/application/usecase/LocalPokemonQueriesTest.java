package com.example.pokedex.application.usecase;

import static com.example.pokedex.application.usecase.FakeLocalPokemonRepository.detailWithId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.domain.exception.InvalidRequestException;
import com.example.pokedex.domain.exception.LocalPokemonNotFoundException;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.shared.Page;
import org.junit.jupiter.api.Test;

/** Reading the local Pokémon: list, view, and the "imported" indicator of the catalog detail. */
class LocalPokemonQueriesTest {

	private final FakeLocalPokemonRepository repository = new FakeLocalPokemonRepository();

	@Test
	void listsTheLocalPokemonPageByPageInPokedexOrder() {
		for (int id : new int[] {25, 1, 4}) {
			repository.add(LocalPokemon.importFrom(detailWithId(id)));
		}

		Page<LocalPokemon> page = new ListLocalPokemonUseCase(repository).list(0, 2);

		assertThat(page.items()).extracting(LocalPokemon::pokeapiId).containsExactly(1, 4);
		assertThat(page.totalItems()).isEqualTo(3);
		assertThat(page.totalPages()).isEqualTo(2);
	}

	@Test
	void rejectsInvalidPaging() {
		ListLocalPokemonUseCase useCase = new ListLocalPokemonUseCase(repository);

		assertThatThrownBy(() -> useCase.list(-1, 20)).isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> useCase.list(0, 51)).isInstanceOf(InvalidRequestException.class);
	}

	@Test
	void returnsOneLocalPokemon() {
		LocalPokemon saved = repository.add(LocalPokemon.importFrom(detailWithId(25)));

		assertThat(new GetLocalPokemonUseCase(repository).get(saved.id())).isEqualTo(saved);
	}

	@Test
	void aMissingLocalPokemonIsNotFound() {
		// For example, another user deleted it.
		assertThatThrownBy(() -> new GetLocalPokemonUseCase(repository).get(99))
				.isInstanceOf(LocalPokemonNotFoundException.class);
	}

	@Test
	void theImportStatusGivesTheLocalIdOfAnImportedPokemon() {
		LocalPokemon saved = repository.add(LocalPokemon.importFrom(detailWithId(25)));
		GetImportStatusUseCase useCase = new GetImportStatusUseCase(repository);

		assertThat(useCase.localIdOf(25)).contains(saved.id());
		assertThat(useCase.localIdOf(1)).isEmpty();
	}
}
