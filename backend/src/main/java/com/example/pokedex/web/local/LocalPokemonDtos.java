package com.example.pokedex.web.local;

import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.web.catalog.PokemonDetailResponse.EvolutionStageResponse;
import com.example.pokedex.web.catalog.PokemonDetailResponse.StatResponse;
import com.example.pokedex.web.error.ErrorResponse;
import com.example.pokedex.web.error.InvalidFieldsException;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Request and response bodies of the local Pokémon API.
 */
final class LocalPokemonDtos {

	private LocalPokemonDtos() {
	}

	/** @param idOrName the Pokémon's number or name in PokeAPI, e.g. "25" or "pikachu" */
	record ImportRequest(@NotBlank String idOrName) {
	}

	/**
	 * The new proprietary fields of a local Pokémon, with the version the user loaded. It replaces all three
	 * fields: a missing or {@code null} local name or region clears it.
	 * <p>
	 * Any other field (e.g. {@code name} or {@code types}) is collected instead of silently ignored, so the
	 * client is told that it can't be changed.
	 */
	record UpdateRequest(
			String localName,
			String localRegion,
			@NotNull(message = "is required") List<String> tags,
			@NotNull(message = "is required") Long version,
			@JsonAnySetter @Schema(hidden = true) Map<String, Object> otherFields) {

		private static final Set<String> FROM_POKEAPI = Set.of("name", "imageUrl", "spriteUrl", "category", "types",
				"heightM", "weightKg", "abilities", "stats", "description", "evolution");
		private static final Set<String> MANAGED_BY_THE_SYSTEM = Set.of("id", "pokeapiId", "createdAt", "updatedAt");

		/** @throws InvalidFieldsException if the body has any field other than the editable ones */
		void rejectOtherFields() {
			if (otherFields == null || otherFields.isEmpty()) {
				return;
			}
			throw new InvalidFieldsException(otherFields.keySet().stream()
					.map(field -> new ErrorResponse.FieldError(field, reasonFor(field)))
					.toList());
		}

		private static String reasonFor(String field) {
			if (FROM_POKEAPI.contains(field)) {
				return "is read-only: it comes from PokeAPI";
			}
			return MANAGED_BY_THE_SYSTEM.contains(field) ? "is read-only" : "is not a known field";
		}
	}

	/**
	 * A local Pokémon: the PokeAPI data copied at import (read-only), the proprietary fields and the
	 * version to send back when changing it.
	 *
	 * @param id          the local id
	 * @param pokeapiId   the Pokémon's number in PokeAPI
	 * @param localName   {@code null} when not set
	 * @param localRegion {@code null} when not set
	 */
	record LocalPokemonResponse(
			long id,
			int pokeapiId,
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
			String localName,
			String localRegion,
			List<String> tags,
			long version,
			Instant createdAt,
			Instant updatedAt) {

		static LocalPokemonResponse from(LocalPokemon pokemon) {
			var data = pokemon.pokeApiData();
			return new LocalPokemonResponse(pokemon.id(), data.id(), data.name(), data.imageUrl(), data.spriteUrl(),
					data.category(), data.types(), data.heightM(), data.weightKg(), data.abilities(),
					data.stats().stream().map(StatResponse::from).toList(), data.description(),
					data.evolution().stream().map(EvolutionStageResponse::from).toList(),
					pokemon.localName(), pokemon.localRegion(), pokemon.tags(), pokemon.version(),
					pokemon.createdAt(), pokemon.updatedAt());
		}
	}

	/** An entry of the local list: what the list screen shows, without stats, description or evolution. */
	record LocalPokemonSummaryResponse(
			long id,
			int pokeapiId,
			String name,
			String spriteUrl,
			String category,
			List<String> types,
			String localName,
			String localRegion,
			List<String> tags) {

		static LocalPokemonSummaryResponse from(LocalPokemon pokemon) {
			var data = pokemon.pokeApiData();
			return new LocalPokemonSummaryResponse(pokemon.id(), data.id(), data.name(), data.spriteUrl(),
					data.category(), data.types(), pokemon.localName(), pokemon.localRegion(), pokemon.tags());
		}
	}
}
