package com.example.pokedex.web.local;

import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.web.catalog.PokemonDetailResponse.EvolutionStageResponse;
import com.example.pokedex.web.catalog.PokemonDetailResponse.StatResponse;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.List;

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
