package com.example.pokedex.infrastructure.pokeapi;

import com.example.pokedex.domain.catalog.EvolutionChain;
import com.example.pokedex.domain.catalog.FlavorText;
import com.example.pokedex.domain.catalog.LocalizedText;
import com.example.pokedex.domain.catalog.Measurements;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.PokemonSummary;
import com.example.pokedex.domain.catalog.Stat;
import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.infrastructure.pokeapi.dto.EvolutionChainDto;
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
				spriteUrl(pokemon),
				englishGenus(species),
				types(pokemon),
				Measurements.hectogramsToKilograms(pokemon.weight()),
				abilities(pokemon));
	}

	static PokemonDetail toDetail(PokemonDto pokemon, SpeciesDto species, EvolutionChainDto chain) {
		return new PokemonDetail(
				pokemon.id(),
				pokemon.name(),
				spriteUrl(pokemon),
				pokemon.sprites() == null ? null : pokemon.sprites().officialArtwork(),
				englishGenus(species),
				types(pokemon),
				Measurements.decimetresToMetres(pokemon.height()),
				Measurements.hectogramsToKilograms(pokemon.weight()),
				abilities(pokemon),
				pokemon.stats().stream()
						.map(stat -> new Stat(stat.stat().name(), stat.baseStat()))
						.toList(),
				FlavorText.description(species.flavorTextEntries().stream()
						.filter(entry -> entry.language() != null)
						.map(entry -> new LocalizedText(entry.language().name(), entry.flavorText()))
						.toList()),
				EvolutionChain.flatten(toNode(chain.chain())));
	}

	/**
	 * Reads the numeric id at the end of a PokeAPI URL (e.g. {@code .../pokemon-species/3/} gives 3).
	 * Only the id is used, so we never call a URL taken from a response.
	 */
	static int idFromUrl(String url) {
		Matcher matcher = url == null ? null : RESOURCE_ID.matcher(url);
		if (matcher == null || !matcher.find()) {
			throw new CatalogUnavailableException("PokeAPI returned an unexpected resource URL");
		}
		return Integer.parseInt(matcher.group(1));
	}

	static int idFromUrl(NamedResource resource) {
		return idFromUrl(resource == null ? null : resource.url());
	}

	private static EvolutionChain.Node toNode(EvolutionChainDto.ChainLink link) {
		return new EvolutionChain.Node(
				idFromUrl(link.species()),
				link.species().name(),
				link.evolvesTo().stream().map(PokeApiMapper::toNode).toList());
	}

	private static String spriteUrl(PokemonDto pokemon) {
		return pokemon.sprites() == null ? null : pokemon.sprites().frontDefault();
	}

	private static List<String> types(PokemonDto pokemon) {
		return pokemon.types().stream()
				.sorted(Comparator.comparingInt(PokemonDto.TypeSlot::slot))
				.map(slot -> slot.type().name())
				.toList();
	}

	private static List<String> abilities(PokemonDto pokemon) {
		return pokemon.abilities().stream()
				.sorted(Comparator.comparingInt(PokemonDto.AbilitySlot::slot))
				.map(slot -> slot.ability().name())
				.toList();
	}

	private static String englishGenus(SpeciesDto species) {
		List<LocalizedText> genera = species.genera().stream()
				.filter(g -> g.language() != null)
				.map(g -> new LocalizedText(g.language().name(), g.genus()))
				.toList();
		return LocalizedText.englishOrEmpty(genera);
	}
}
