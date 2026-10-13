package com.example.pokedex.infrastructure.pokeapi;

import com.example.pokedex.infrastructure.pokeapi.dto.PokemonDto;
import com.example.pokedex.infrastructure.pokeapi.dto.SpeciesDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * In-memory cache in front of {@link PokeApiClient}.
 * <ul>
 * <li>Limited size, entries expire after the configured time.</li>
 * <li>Errors and "not found" are never cached: a failing load stores nothing.</li>
 * <li>One entry per Pokémon: details are stored by id; names ("Pikachu", "pikachu") only point to that id.</li>
 * <li>Simultaneous requests for the same uncached key make a single PokeAPI call (Caffeine computes each key
 * atomically).</li>
 * </ul>
 * Caffeine is used directly rather than through {@code @Cacheable} because the key must be normalized
 * (name or number) before the lookup, which a single cache annotation cannot express.
 */
public class PokeApiCache {

	private static final Pattern NUMBER = Pattern.compile("\\d{1,9}");

	private final PokeApiClient client;
	private final Cache<Integer, PokemonDto> pokemonById;
	private final Cache<String, Integer> pokemonIdByName;
	private final Cache<Integer, SpeciesDto> speciesById;

	public PokeApiCache(PokeApiClient client, PokeApiProperties.CacheSettings settings, Ticker ticker) {
		this.client = client;
		this.pokemonById = newCache(settings, ticker);
		this.pokemonIdByName = newCache(settings, ticker);
		this.speciesById = newCache(settings, ticker);
	}

	/**
	 * @param idOrName a Pokémon number or name, in any case
	 */
	public PokemonDto pokemon(String idOrName) {
		String key = idOrName.trim().toLowerCase(Locale.ROOT);
		int id = NUMBER.matcher(key).matches()
				? Integer.parseInt(key)
				: pokemonIdByName.get(key, this::loadIdByName);
		return pokemonById.get(id, this::loadById);
	}

	public SpeciesDto species(int id) {
		return speciesById.get(id, client::getSpecies);
	}

	private PokemonDto loadById(int id) {
		PokemonDto pokemon = client.getPokemon(String.valueOf(id));
		pokemonIdByName.put(pokemon.name(), pokemon.id());
		return pokemon;
	}

	private int loadIdByName(String name) {
		PokemonDto pokemon = client.getPokemon(name);
		pokemonById.put(pokemon.id(), pokemon);
		return pokemon.id();
	}

	private static <K, V> Cache<K, V> newCache(PokeApiProperties.CacheSettings settings, Ticker ticker) {
		return Caffeine.newBuilder()
				.maximumSize(settings.maxSize())
				.expireAfterWrite(settings.ttl())
				.ticker(ticker)
				.build();
	}
}
