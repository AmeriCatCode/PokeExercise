package com.example.pokedex.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.domain.catalog.EvolutionStage;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.Stat;
import com.example.pokedex.domain.exception.InvalidRequestException;
import com.example.pokedex.domain.exception.PokemonNotFoundException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GetPokemonDetailUseCaseTest {

	private final PokemonCatalogPort catalog = mock(PokemonCatalogPort.class);
	private final GetPokemonDetailUseCase useCase = new GetPokemonDetailUseCase(catalog);

	@Test
	void returnsTheDetailGivenByTheCatalog() {
		PokemonDetail bulbasaur = new PokemonDetail(1, "bulbasaur", "sprite.png", "artwork.png", "Seed Pokémon",
				List.of("grass", "poison"), 0.7, 6.9, List.of("overgrow"), List.of(new Stat("hp", 45)),
				"A strange seed.", List.of(new EvolutionStage(1, 1, "bulbasaur", null)));
		when(catalog.getPokemonDetail("bulbasaur")).thenReturn(bulbasaur);

		assertThat(useCase.get("bulbasaur")).isEqualTo(bulbasaur);
	}

	@Test
	void propagatesNotFoundForAnUnknownPokemon() {
		when(catalog.getPokemonDetail("missingno")).thenThrow(new PokemonNotFoundException("missingno"));

		assertThatThrownBy(() -> useCase.get("missingno")).isInstanceOf(PokemonNotFoundException.class);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", "  ", "../admin", "pika chu", "pikachu?x=1"})
	void rejectsAnInvalidIdOrNameWithoutCallingTheCatalog(String idOrName) {
		assertThatThrownBy(() -> useCase.get(idOrName))
				.isInstanceOf(InvalidRequestException.class)
				.extracting("field").isEqualTo("idOrName");

		verify(catalog, never()).getPokemonDetail(anyString());
	}

	@Test
	void rejectsAnIdOrNameThatIsTooLong() {
		assertThatThrownBy(() -> useCase.get("a".repeat(101))).isInstanceOf(InvalidRequestException.class);
	}
}
