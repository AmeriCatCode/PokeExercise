package com.example.pokedex.domain.local;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.pokedex.domain.catalog.EvolutionStage;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.Stat;
import java.util.List;
import org.junit.jupiter.api.Test;

class LocalPokemonTest {

	static final PokemonDetail BULBASAUR = new PokemonDetail(1, "bulbasaur", "https://img.example/1.png",
			"https://img.example/artwork/1.png", "Seed Pokémon", List.of("grass", "poison"), 0.7, 6.9,
			List.of("overgrow", "chlorophyll"), List.of(new Stat("hp", 45), new Stat("attack", 49)),
			"A strange seed was planted on its back at birth.",
			List.of(new EvolutionStage(1, 1, "bulbasaur", null), new EvolutionStage(2, 2, "ivysaur", "bulbasaur")));

	@Test
	void aNewlyImportedPokemonHasEmptyProprietaryFields() {
		LocalPokemon imported = LocalPokemon.importFrom(BULBASAUR);

		assertThat(imported.id()).isNull();
		assertThat(imported.localName()).isNull();
		assertThat(imported.localRegion()).isNull();
		assertThat(imported.tags()).isEmpty();
		assertThat(imported.version()).isNull();
	}

	@Test
	void keepsThePokeApiDataAsReceivedIncludingCategoryAndTypes() {
		LocalPokemon imported = LocalPokemon.importFrom(BULBASAUR);

		assertThat(imported.pokeApiData()).isEqualTo(BULBASAUR);
		assertThat(imported.pokeapiId()).isEqualTo(1);
		assertThat(imported.pokeApiData().category()).isEqualTo("Seed Pokémon");
		assertThat(imported.pokeApiData().types()).containsExactly("grass", "poison");
	}

	@Test
	void anUpdateChangesOnlyTheProprietaryFields() {
		LocalPokemon imported = new LocalPokemon(7L, BULBASAUR, null, null, List.of(), 3L, null, null);

		LocalPokemon updated = imported.withProprietaryFields(
				new ProprietaryFields("Bulbizarre", "Kanto", List.of("starter")));

		assertThat(updated.localName()).isEqualTo("Bulbizarre");
		assertThat(updated.localRegion()).isEqualTo("Kanto");
		assertThat(updated.tags()).containsExactly("starter");
		assertThat(updated.id()).isEqualTo(7L);
		assertThat(updated.pokeApiData()).isEqualTo(BULBASAUR);
		assertThat(updated.version()).isEqualTo(3L);
	}

	@Test
	void tagsCannotBeChangedFromOutside() {
		List<String> tags = new java.util.ArrayList<>(List.of("starter"));
		LocalPokemon pokemon = new LocalPokemon(7L, BULBASAUR, "Bulbizarre", "Kanto", tags, 0L, null, null);

		tags.add("changed");

		assertThat(pokemon.tags()).containsExactly("starter");
	}
}
