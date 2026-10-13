package com.example.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** {@code GET /pokemon/{idOrName}}: only the fields we use. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonDto(
		int id,
		String name,
		int height,
		int weight,
		Sprites sprites,
		List<TypeSlot> types,
		List<AbilitySlot> abilities,
		List<StatValue> stats,
		NamedResource species) {

	public PokemonDto {
		types = types == null ? List.of() : types;
		abilities = abilities == null ? List.of() : abilities;
		stats = stats == null ? List.of() : stats;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Sprites(@JsonProperty("front_default") String frontDefault, OtherSprites other) {

		/** The full-size official artwork, or {@code null}. */
		public String officialArtwork() {
			return other == null || other.officialArtwork() == null ? null : other.officialArtwork().frontDefault();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record OtherSprites(@JsonProperty("official-artwork") Artwork officialArtwork) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Artwork(@JsonProperty("front_default") String frontDefault) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TypeSlot(int slot, NamedResource type) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record AbilitySlot(int slot, NamedResource ability) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record StatValue(@JsonProperty("base_stat") int baseStat, NamedResource stat) {
	}
}
