package com.example.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** {@code GET /evolution-chain/{id}}: the evolution tree, starting from the base species. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EvolutionChainDto(int id, ChainLink chain) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ChainLink(NamedResource species, @JsonProperty("evolves_to") List<ChainLink> evolvesTo) {

		public ChainLink {
			evolvesTo = evolvesTo == null ? List.of() : evolvesTo;
		}
	}
}
