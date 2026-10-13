package com.example.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** {@code GET /pokemon?offset=&limit=}: one page of names and URLs. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PokemonListDto(long count, List<NamedResource> results) {

	public PokemonListDto {
		results = results == null ? List.of() : results;
	}
}
