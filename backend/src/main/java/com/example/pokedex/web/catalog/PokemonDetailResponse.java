package com.example.pokedex.web.catalog;

import com.example.pokedex.domain.catalog.EvolutionStage;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.Stat;
import java.util.List;

/**
 * The detail of one Pokémon: image, category, types, core statistics, description and evolution lineage.
 *
 * @param imageUrl  the full-size official artwork; {@code null} when there is none
 * @param spriteUrl {@code null} when there is none
 */
public record PokemonDetailResponse(
		int id,
		String name,
		String imageUrl,
		String spriteUrl,
		String category,
		List<String> types,
		double heightM,
		double weightKg,
		List<String> abilities,
		List<StatResponse> stats,
		String description,
		List<EvolutionStageResponse> evolution) {

	public record StatResponse(String name, int baseValue) {

		static StatResponse from(Stat stat) {
			return new StatResponse(stat.name(), stat.baseValue());
		}
	}

	/**
	 * @param stage       1 for the base species, 2 for its evolutions, and so on
	 * @param evolvesFrom {@code null} for the base species
	 */
	public record EvolutionStageResponse(int stage, int speciesId, String name, String evolvesFrom) {

		static EvolutionStageResponse from(EvolutionStage stage) {
			return new EvolutionStageResponse(stage.stage(), stage.speciesId(), stage.name(), stage.evolvesFrom());
		}
	}

	static PokemonDetailResponse from(PokemonDetail detail) {
		return new PokemonDetailResponse(detail.id(), detail.name(), detail.imageUrl(), detail.spriteUrl(),
				detail.category(), detail.types(), detail.heightM(), detail.weightKg(), detail.abilities(),
				detail.stats().stream().map(StatResponse::from).toList(),
				detail.description(),
				detail.evolution().stream().map(EvolutionStageResponse::from).toList());
	}
}
