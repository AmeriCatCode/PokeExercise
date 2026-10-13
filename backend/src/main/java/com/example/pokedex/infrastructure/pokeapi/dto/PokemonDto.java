package com.example.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** {@code GET /pokemon/{idOrName}}: only the fields we use. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonDto(
		int id,
		String name,
		int weight,
		Sprites sprites,
		List<TypeSlot> types,
		List<AbilitySlot> abilities,
		NamedResource species) {

	public PokemonDto {
		types = types == null ? List.of() : types;
		abilities = abilities == null ? List.of() : abilities;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Sprites(@JsonProperty("front_default") String frontDefault) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TypeSlot(int slot, NamedResource type) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record AbilitySlot(int slot, NamedResource ability) {
	}
}
