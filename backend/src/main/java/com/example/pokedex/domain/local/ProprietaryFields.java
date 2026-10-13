package com.example.pokedex.domain.local;

import com.example.pokedex.domain.exception.InvalidRequestException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The fields of a local Pokémon that users may change (US-04), validated and normalized. Always valid
 * once created.
 *
 * @param localName   trimmed; {@code null} when not set (a blank value clears it)
 * @param localRegion trimmed; {@code null} when not set (a blank value clears it)
 * @param tags        trimmed, lower case, each one only once
 */
public record ProprietaryFields(String localName, String localRegion, List<String> tags) {

	public static final int MAX_TEXT_LENGTH = 100;
	public static final int MAX_TAG_LENGTH = 30;
	public static final int MAX_TAGS = 20;

	public ProprietaryFields {
		localName = optionalText(localName, "localName");
		localRegion = optionalText(localRegion, "localRegion");
		tags = normalizeTags(tags);
	}

	private static String optionalText(String value, String field) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.strip();
		if (trimmed.length() > MAX_TEXT_LENGTH) {
			throw new InvalidRequestException(field, "must be at most " + MAX_TEXT_LENGTH + " characters");
		}
		return trimmed;
	}

	private static List<String> normalizeTags(List<String> tags) {
		if (tags == null) {
			return List.of();
		}
		if (tags.size() > MAX_TAGS) {
			throw new InvalidRequestException("tags", "must contain at most " + MAX_TAGS + " tags");
		}
		List<String> normalized = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (String tag : tags) {
			if (tag == null || tag.isBlank()) {
				throw new InvalidRequestException("tags", "must not contain blank tags");
			}
			String value = tag.strip().toLowerCase(Locale.ROOT);
			if (value.length() > MAX_TAG_LENGTH) {
				throw new InvalidRequestException("tags", "must each be at most " + MAX_TAG_LENGTH + " characters");
			}
			if (!seen.add(value)) {
				throw new InvalidRequestException("tags", "must not contain the same tag twice (\"" + value + "\")");
			}
			normalized.add(value);
		}
		return List.copyOf(normalized);
	}
}
