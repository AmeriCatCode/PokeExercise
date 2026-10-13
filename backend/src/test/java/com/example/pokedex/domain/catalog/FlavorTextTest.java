package com.example.pokedex.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class FlavorTextTest {

	@Test
	void picksTheMostRecentEnglishText() {
		List<LocalizedText> entries = List.of(
				new LocalizedText("en", "Old text."),
				new LocalizedText("fr", "Texte récent."),
				new LocalizedText("en", "Recent text."),
				new LocalizedText("ja", "最新"));

		assertThat(FlavorText.description(entries)).isEqualTo("Recent text.");
	}

	@Test
	void replacesTheLineAndPageBreaksOfTheGameTexts() {
		List<LocalizedText> entries = List.of(new LocalizedText("en",
				"A strange seed was\nplanted on its\nback at birth.\fThe plant sprouts\nand grows with\nthis POKéMON."));

		assertThat(FlavorText.description(entries))
				.isEqualTo("A strange seed was planted on its back at birth. The plant sprouts and grows with this POKéMON.");
	}

	@Test
	void joinsWordsSplitBySoftHyphensAtTheEndOfALine() {
		List<LocalizedText> entries = List.of(new LocalizedText("en", "It can go for days without eat­\ning a single morsel."));

		assertThat(FlavorText.description(entries)).isEqualTo("It can go for days without eating a single morsel.");
	}

	@Test
	void isEmptyWhenThereIsNoEnglishText() {
		assertThat(FlavorText.description(List.of(new LocalizedText("de", "Text.")))).isEmpty();
	}
}
