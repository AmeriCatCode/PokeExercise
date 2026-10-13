package com.example.pokedex.web.catalog;

import com.example.pokedex.domain.catalog.EvolutionStage;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.Stat;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Optional;

/**
 * The detail of one Pokémon: image, category, types, core statistics, description and evolution lineage.
 *
 * @param imageUrl  the full-size official artwork; {@code null} when there is none
 * @param spriteUrl {@code null} when there is none
 * @param imported  logged-in users only: whether the Pokémon is in the local database; left out for visitors
 * @param localId   logged-in users only: the local id of the imported Pokémon; left out when not imported
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
		List<EvolutionStageResponse> evolution,
		@JsonInclude(JsonInclude.Include.NON_NULL) Boolean imported,
		@JsonInclude(JsonInclude.Include.NON_NULL) Long localId) {

	public record StatResponse(String name, int baseValue) {

		public static StatResponse from(Stat stat) {
			return new StatResponse(stat.name(), stat.baseValue());
		}
	}

	/**
	 * @param stage       1 for the base species, 2 for its evolutions, and so on
	 * @param evolvesFrom {@code null} for the base species
	 */
	public record EvolutionStageResponse(int stage, int speciesId, String name, String evolvesFrom) {

		public static EvolutionStageResponse from(EvolutionStage stage) {
			return new EvolutionStageResponse(stage.stage(), stage.speciesId(), stage.name(), stage.evolvesFrom());
		}
	}

	/** For a visitor: no import information. */
	static PokemonDetailResponse from(PokemonDetail detail) {
		return from(detail, null, null);
	}

	/** For a logged-in user: the PokeAPI data plus whether it is imported. */
	static PokemonDetailResponse from(PokemonDetail detail, Optional<Long> localId) {
		return from(detail, localId.isPresent(), localId.orElse(null));
	}

	private static PokemonDetailResponse from(PokemonDetail detail, Boolean imported, Long localId) {
		return new PokemonDetailResponse(detail.id(), detail.name(), detail.imageUrl(), detail.spriteUrl(),
				detail.category(), detail.types(), detail.heightM(), detail.weightKg(), detail.abilities(),
				detail.stats().stream().map(StatResponse::from).toList(),
				detail.description(),
				detail.evolution().stream().map(EvolutionStageResponse::from).toList(),
				imported, localId);
	}
}
