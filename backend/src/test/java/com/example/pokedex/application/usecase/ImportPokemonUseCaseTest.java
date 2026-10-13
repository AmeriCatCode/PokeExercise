package com.example.pokedex.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.domain.catalog.EvolutionStage;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.Stat;
import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.domain.exception.InvalidRequestException;
import com.example.pokedex.domain.exception.PokemonAlreadyImportedException;
import com.example.pokedex.domain.exception.PokemonNotFoundException;
import com.example.pokedex.domain.local.LocalPokemon;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class ImportPokemonUseCaseTest {

	static final PokemonDetail PIKACHU = new PokemonDetail(25, "pikachu", "sprite.png", "artwork.png",
			"Mouse Pokémon", List.of("electric"), 0.4, 6.0, List.of("static"), List.of(new Stat("hp", 35)),
			"It stores electricity in its cheeks.", List.of(new EvolutionStage(1, 172, "pichu", null),
					new EvolutionStage(2, 25, "pikachu", "pichu")));

	private final PokemonCatalogPort catalog = mock(PokemonCatalogPort.class);
	private final FakeLocalPokemonRepository repository = spy(new FakeLocalPokemonRepository());
	private final ImportPokemonUseCase useCase = new ImportPokemonUseCase(catalog, repository);

	@Test
	void fetchesThePokemonFromTheCatalogAndSavesIt() {
		when(catalog.getPokemonDetail("Pikachu")).thenReturn(PIKACHU);

		LocalPokemon imported = useCase.importPokemon("Pikachu");

		assertThat(imported.id()).isNotNull();
		assertThat(imported.pokeApiData()).isEqualTo(PIKACHU);
		assertThat(imported.localName()).isNull();
		assertThat(imported.localRegion()).isNull();
		assertThat(imported.tags()).isEmpty();
		assertThat(repository.all()).containsExactly(imported);
	}

	@Test
	void rejectsAPokemonThatIsAlreadyImported() {
		when(catalog.getPokemonDetail(anyString())).thenReturn(PIKACHU);
		useCase.importPokemon("pikachu");

		// Another name for the same Pokémon is still the same Pokémon.
		assertThatThrownBy(() -> useCase.importPokemon("25"))
				.isInstanceOf(PokemonAlreadyImportedException.class)
				.extracting("pokeapiId").isEqualTo(25);
		assertThat(repository.all()).hasSize(1);
	}

	@Test
	void rejectsAPokemonThatIsNotInTheCatalog() {
		when(catalog.getPokemonDetail("missingno")).thenThrow(new PokemonNotFoundException("missingno"));

		assertThatThrownBy(() -> useCase.importPokemon("missingno")).isInstanceOf(PokemonNotFoundException.class);
		assertThat(repository.all()).isEmpty();
	}

	@Test
	void savesNothingWhenTheCatalogIsUnavailable() {
		when(catalog.getPokemonDetail("pikachu")).thenThrow(new CatalogUnavailableException("timeout"));

		assertThatThrownBy(() -> useCase.importPokemon("pikachu")).isInstanceOf(CatalogUnavailableException.class);
		verify(repository, never()).add(any());
	}

	@Test
	void rejectsAnInvalidIdOrNameWithoutCallingTheCatalog() {
		assertThatThrownBy(() -> useCase.importPokemon("../pokemon"))
				.isInstanceOf(InvalidRequestException.class)
				.extracting("field").isEqualTo("idOrName");
		verify(catalog, never()).getPokemonDetail(anyString());
	}

	@Test
	void callsTheCatalogFirstAndOnlyThenTheDatabase() {
		// The slow PokeAPI call must never run while a database transaction (and its connection) is open:
		// the repository opens its own short transaction, so everything it does comes after the catalog.
		// LocalPokemonApiIntegrationTest checks the same with the real transaction manager.
		when(catalog.getPokemonDetail("pikachu")).thenReturn(PIKACHU);

		useCase.importPokemon("pikachu");

		InOrder order = inOrder(catalog, repository);
		order.verify(catalog).getPokemonDetail("pikachu");
		order.verify(repository).add(any());
	}
}
