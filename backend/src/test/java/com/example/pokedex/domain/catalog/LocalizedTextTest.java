package com.example.pokedex.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LocalizedTextTest {

	@Test
	void picksTheEnglishText() {
		List<LocalizedText> genera = List.of(
				new LocalizedText("ja", "たねポケモン"),
				new LocalizedText("en", "Seed Pokémon"),
				new LocalizedText("fr", "Pokémon Graine"));

		assertThat(LocalizedText.englishOrEmpty(genera)).isEqualTo("Seed Pokémon");
	}

	@Test
	void isEmptyWhenThereIsNoEnglishText() {
		assertThat(LocalizedText.englishOrEmpty(List.of(new LocalizedText("fr", "Pokémon Graine")))).isEmpty();
		assertThat(LocalizedText.englishOrEmpty(List.of())).isEmpty();
	}
}
