package com.example.pokedex.domain.catalog;

import com.example.pokedex.domain.exception.InvalidRequestException;
import java.util.regex.Pattern;

/**
 * How a user names a Pokémon of the catalog: its number or its name, e.g. "25" or "pikachu".
 */
public final class IdOrName {

	/** PokeAPI names and numbers only use letters, digits and hyphens (e.g. "mr-mime", "25"). */
	private static final Pattern ID_OR_NAME = Pattern.compile("[A-Za-z0-9-]{1,100}");

	private IdOrName() {
	}

	/**
	 * Checked before anything is sent to PokeAPI, so a value can't change the path it calls.
	 *
	 * @throws InvalidRequestException if the value can't be a Pokémon number or name
	 */
	public static String requireValid(String idOrName) {
		if (idOrName == null || !ID_OR_NAME.matcher(idOrName).matches()) {
			throw new InvalidRequestException("idOrName",
					"must be a Pokémon number or name (letters, digits and hyphens, up to 100 characters)");
		}
		return idOrName;
	}
}
