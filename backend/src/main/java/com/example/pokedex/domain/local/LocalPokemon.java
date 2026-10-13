package com.example.pokedex.domain.local;

import com.example.pokedex.domain.catalog.PokemonDetail;
import java.time.Instant;
import java.util.List;

/**
 * A Pokémon imported into the local database (US-03): the PokeAPI data, copied once at import and never
 * changed afterwards, plus the proprietary fields our users maintain.
 *
 * @param id          our own key; {@code null} until saved
 * @param pokeApiData the PokeAPI data as received at import (read-only)
 * @param localName   localized nomenclature; {@code null} when not set
 * @param localRegion geographical metadata; {@code null} when not set
 * @param tags        internal classification tags
 * @param version     incremented on every change, to detect concurrent edits; {@code null} until saved
 * @param createdAt   {@code null} until saved
 * @param updatedAt   {@code null} until saved
 */
public record LocalPokemon(
		Long id,
		PokemonDetail pokeApiData,
		String localName,
		String localRegion,
		List<String> tags,
		Long version,
		Instant createdAt,
		Instant updatedAt) {

	public LocalPokemon {
		tags = List.copyOf(tags);
	}

	/** A new local copy of a PokeAPI Pokémon, with empty proprietary fields. */
	public static LocalPokemon importFrom(PokemonDetail pokeApiData) {
		return new LocalPokemon(null, pokeApiData, null, null, List.of(), null, null, null);
	}

	/** The Pokémon's number in PokeAPI: unique in the local database. */
	public int pokeapiId() {
		return pokeApiData.id();
	}
}
