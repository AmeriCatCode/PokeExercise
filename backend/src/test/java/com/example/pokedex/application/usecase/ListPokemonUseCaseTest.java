package com.example.pokedex.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.domain.catalog.PokemonSummary;
import com.example.pokedex.domain.exception.InvalidRequestException;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.domain.shared.PageRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListPokemonUseCaseTest {

	private final PokemonCatalogPort catalog = mock(PokemonCatalogPort.class);
	private final ListPokemonUseCase useCase = new ListPokemonUseCase(catalog);

	@Test
	void returnsThePageGivenByTheCatalog() {
		PokemonSummary bulbasaur = new PokemonSummary(1, "bulbasaur", "sprite.png", "Seed Pokémon",
				List.of("grass", "poison"), 6.9, List.of("overgrow", "chlorophyll"));
		Page<PokemonSummary> page = new Page<>(List.of(bulbasaur), 0, 20, 1302);
		when(catalog.listPokemon(new PageRequest(0, 20))).thenReturn(page);

		assertThat(useCase.list(0, 20)).isEqualTo(page);
	}

	@Test
	void rejectsAnInvalidPageWithoutCallingTheCatalog() {
		assertThatThrownBy(() -> useCase.list(-1, 20)).isInstanceOf(InvalidRequestException.class);
		assertThatThrownBy(() -> useCase.list(0, 51)).isInstanceOf(InvalidRequestException.class);

		verify(catalog, never()).listPokemon(any());
	}
}
