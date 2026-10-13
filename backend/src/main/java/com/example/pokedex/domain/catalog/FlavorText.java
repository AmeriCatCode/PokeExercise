package com.example.pokedex.domain.catalog;

import java.util.List;
import java.util.regex.Pattern;

/**
 * The narrative description of a Pokémon, taken from the Pokédex texts of the games.
 */
public final class FlavorText {

	private static final String ENGLISH = "en";
	private static final Pattern SOFT_HYPHEN_LINE_BREAK = Pattern.compile("­\\s*");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private FlavorText() {
	}

	/**
	 * The most recent English text (texts are listed from the oldest game to the newest), as one clean line:
	 * the game texts contain line breaks, page breaks and soft hyphens for their text boxes.
	 * Empty when there is no English text.
	 */
	public static String description(List<LocalizedText> entries) {
		String latest = entries.stream()
				.filter(entry -> ENGLISH.equals(entry.language()))
				.map(LocalizedText::text)
				.reduce((first, second) -> second)
				.orElse("");
		String joined = SOFT_HYPHEN_LINE_BREAK.matcher(latest).replaceAll("");
		return WHITESPACE.matcher(joined).replaceAll(" ").trim();
	}
}
