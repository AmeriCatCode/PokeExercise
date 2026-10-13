package com.example.pokedex.web.catalog;

import com.example.pokedex.domain.catalog.PokemonSummary;
import java.util.List;

/**
 * A catalog entry: sprite, category, types, mass and skills.
 *
 * @param spriteUrl {@code null} when the Pokémon has no sprite
 */
public record PokemonSummaryResponse(
		int id,
		String name,
		String spriteUrl,
		String category,
		List<String> types,
		double weightKg,
		List<String> abilities) {

	static PokemonSummaryResponse from(PokemonSummary pokemon) {
		return new PokemonSummaryResponse(pokemon.id(), pokemon.name(), pokemon.spriteUrl(), pokemon.category(),
				pokemon.types(), pokemon.weightKg(), pokemon.abilities());
	}
}
