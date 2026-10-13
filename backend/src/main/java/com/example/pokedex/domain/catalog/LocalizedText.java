package com.example.pokedex.domain.catalog;

import java.util.List;

/**
 * A text in a given language (PokeAPI language code, e.g. "en").
 */
public record LocalizedText(String language, String text) {

	private static final String ENGLISH = "en";

	/** Returns the English text of the list, or an empty string when there is none. */
	public static String englishOrEmpty(List<LocalizedText> texts) {
		return texts.stream()
				.filter(t -> ENGLISH.equals(t.language()))
				.map(LocalizedText::text)
				.findFirst()
				.orElse("");
	}
}
