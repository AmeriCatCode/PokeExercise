package com.example.pokedex.domain.catalog;

import java.util.List;

/**
 * A Pokémon as shown in the catalog list (US-01).
 *
 * @param spriteUrl may be {@code null}: some Pokémon forms have no sprite
 * @param category  the official Pokédex category (English genus, e.g. "Seed Pokémon"), or empty
 */
public record PokemonSummary(
		int id,
		String name,
		String spriteUrl,
		String category,
		List<String> types,
		double weightKg,
		List<String> abilities) {

	public PokemonSummary {
		types = List.copyOf(types);
		abilities = List.copyOf(abilities);
	}
}
