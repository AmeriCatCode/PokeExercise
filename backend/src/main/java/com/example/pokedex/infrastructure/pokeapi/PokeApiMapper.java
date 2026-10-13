package com.example.pokedex.infrastructure.pokeapi;

import com.example.pokedex.domain.catalog.LocalizedText;
import com.example.pokedex.domain.catalog.Measurements;
import com.example.pokedex.domain.catalog.PokemonSummary;
import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.infrastructure.pokeapi.dto.NamedResource;
import com.example.pokedex.infrastructure.pokeapi.dto.PokemonDto;
import com.example.pokedex.infrastructure.pokeapi.dto.SpeciesDto;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Maps PokeAPI responses to domain objects.
 */
final class PokeApiMapper {

	private static final Pattern RESOURCE_ID = Pattern.compile("/(\\d+)/?$");

	private PokeApiMapper() {
	}

	static PokemonSummary toSummary(PokemonDto pokemon, SpeciesDto species) {
		return new PokemonSummary(
				pokemon.id(),
				pokemon.name(),
				pokemon.sprites() == null ? null : pokemon.sprites().frontDefault(),
				englishGenus(species),
				pokemon.types().stream()
						.sorted(Comparator.comparingInt(PokemonDto.TypeSlot::slot))
						.map(slot -> slot.type().name())
						.toList(),
				Measurements.hectogramsToKilograms(pokemon.weight()),
				pokemon.abilities().stream()
						.sorted(Comparator.comparingInt(PokemonDto.AbilitySlot::slot))
						.map(slot -> slot.ability().name())
						.toList());
	}

	/**
	 * Reads the numeric id at the end of a PokeAPI URL (e.g. {@code .../pokemon-species/3/} gives 3).
	 * Only the id is used, so we never call a URL taken from a response.
	 */
	static int idFromUrl(NamedResource resource) {
		Matcher matcher = resource == null || resource.url() == null ? null : RESOURCE_ID.matcher(resource.url());
		if (matcher == null || !matcher.find()) {
			throw new CatalogUnavailableException("PokeAPI returned an unexpected resource URL");
		}
		return Integer.parseInt(matcher.group(1));
	}

	private static String englishGenus(SpeciesDto species) {
		List<LocalizedText> genera = species.genera().stream()
				.filter(g -> g.language() != null)
				.map(g -> new LocalizedText(g.language().name(), g.genus()))
				.toList();
		return LocalizedText.englishOrEmpty(genera);
	}
}
