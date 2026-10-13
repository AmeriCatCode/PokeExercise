package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.exception.InvalidRequestException;
import java.util.regex.Pattern;

/**
 * US-02: see everything about one Pokémon.
 */
public class GetPokemonDetailUseCase {

	/** PokeAPI names and numbers only use letters, digits and hyphens (e.g. "mr-mime", "25"). */
	private static final Pattern ID_OR_NAME = Pattern.compile("[A-Za-z0-9-]{1,100}");

	private final PokemonCatalogPort catalog;

	public GetPokemonDetailUseCase(PokemonCatalogPort catalog) {
		this.catalog = catalog;
	}

	public PokemonDetail get(String idOrName) {
		if (idOrName == null || !ID_OR_NAME.matcher(idOrName).matches()) {
			throw new InvalidRequestException("idOrName",
					"must be a Pokémon number or name (letters, digits and hyphens, up to 100 characters)");
		}
		return catalog.getPokemonDetail(idOrName);
	}
}
