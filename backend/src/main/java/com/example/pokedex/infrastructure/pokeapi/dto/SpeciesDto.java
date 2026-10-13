package com.example.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** {@code GET /pokemon-species/{id}}: only the fields we use. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpeciesDto(int id, String name, List<Genus> genera) {

	public SpeciesDto {
		genera = genera == null ? List.of() : genera;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Genus(String genus, NamedResource language) {
	}
}
