package com.example.pokedex.domain.catalog;

import java.util.List;

/**
 * Everything about one Pokémon, combined from several PokeAPI resources (US-02).
 *
 * @param spriteUrl may be {@code null}
 * @param imageUrl  the full-size official artwork; may be {@code null}
 * @param category  the official Pokédex category (English genus), or empty
 * @param description the narrative Pokédex text, or empty
 */
public record PokemonDetail(
		int id,
		String name,
		String spriteUrl,
		String imageUrl,
		String category,
		List<String> types,
		double heightM,
		double weightKg,
		List<String> abilities,
		List<Stat> stats,
		String description,
		List<EvolutionStage> evolution) {

	public PokemonDetail {
		types = List.copyOf(types);
		abilities = List.copyOf(abilities);
		stats = List.copyOf(stats);
		evolution = List.copyOf(evolution);
	}
}
