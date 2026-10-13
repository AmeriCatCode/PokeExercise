package com.example.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** {@code GET /pokemon-species/{id}}: only the fields we use. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SpeciesDto(
		int id,
		String name,
		List<Genus> genera,
		@JsonProperty("flavor_text_entries") List<FlavorTextEntry> flavorTextEntries,
		@JsonProperty("evolution_chain") ApiResource evolutionChain) {

	public SpeciesDto {
		genera = genera == null ? List.of() : genera;
		flavorTextEntries = flavorTextEntries == null ? List.of() : flavorTextEntries;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Genus(String genus, NamedResource language) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record FlavorTextEntry(@JsonProperty("flavor_text") String flavorText, NamedResource language) {
	}

	/** A reference to another resource that has a URL only. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ApiResource(String url) {
	}
}
