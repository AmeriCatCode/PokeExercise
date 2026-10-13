package com.example.pokedex.infrastructure.pokeapi;

import com.example.pokedex.infrastructure.pokeapi.dto.PokemonDto;
import com.example.pokedex.infrastructure.pokeapi.dto.SpeciesDto;
import com.github.benmanes.caffeine.cache.AsyncCache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * In-memory cache in front of {@link PokeApiClient}.
 * <ul>
 * <li>Limited size, entries expire after the configured time.</li>
 * <li>Errors and "not found" are never cached: a failed load is removed from the cache.</li>
 * <li>One entry per Pokémon: details are stored by id; names ("Pikachu", "pikachu") only point to that id.</li>
 * <li>Simultaneous requests for the same uncached key make a single PokeAPI call: they share the same
 * in-flight load.</li>
 * </ul>
 * Caffeine is used directly rather than through {@code @Cacheable} because the key must be normalized
 * (name or number) before the lookup, which a single cache annotation cannot express.
 * <p>
 * It is an <em>async</em> cache on purpose: a synchronous {@code Cache.get(key, loader)} runs the HTTP call
 * inside a lock, which on Java 21 pins the virtual thread to its carrier thread. With few CPUs that turns the
 * parallel page load into an almost sequential one. Here the lock only stores a future; the call itself runs
 * on its own virtual thread, outside any lock.
 */
public class PokeApiCache {

	private static final Pattern NUMBER = Pattern.compile("\\d{1,9}");

	private final PokeApiClient client;
	private final ExecutorService loader = Executors.newVirtualThreadPerTaskExecutor();
	private final AsyncCache<Integer, PokemonDto> pokemonById;
	private final AsyncCache<String, Integer> pokemonIdByName;
	private final AsyncCache<Integer, SpeciesDto> speciesById;

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
				: get(pokemonIdByName, key, this::loadIdByName);
		return get(pokemonById, id, this::loadById);
	}

	public SpeciesDto species(int id) {
		return get(speciesById, id, client::getSpecies);
	}

	private PokemonDto loadById(int id) {
		PokemonDto pokemon = client.getPokemon(String.valueOf(id));
		pokemonIdByName.put(pokemon.name(), CompletableFuture.completedFuture(pokemon.id()));
		return pokemon;
	}

	private int loadIdByName(String name) {
		PokemonDto pokemon = client.getPokemon(name);
		pokemonById.put(pokemon.id(), CompletableFuture.completedFuture(pokemon));
		return pokemon.id();
	}

	/** Returns the cached value, or loads it on a virtual thread; rethrows the load's own exception. */
	private <K, V> V get(AsyncCache<K, V> cache, K key, Function<K, V> load) {
		CompletableFuture<V> future = cache.get(key,
				(k, executor) -> CompletableFuture.supplyAsync(() -> load.apply(k), loader));
		try {
			return future.join();
		}
		catch (CompletionException ex) {
			// Caffeine also drops failed loads, but asynchronously: remove it now so an immediate retry
			// calls PokeAPI again instead of getting the same failure.
			cache.asMap().remove(key, future);
			if (ex.getCause() instanceof RuntimeException cause) {
				throw cause;
			}
			throw ex;
		}
	}

	private static <K, V> AsyncCache<K, V> newCache(PokeApiProperties.CacheSettings settings, Ticker ticker) {
		return Caffeine.newBuilder()
				.maximumSize(settings.maxSize())
				.expireAfterWrite(settings.ttl())
				.ticker(ticker)
				.buildAsync();
	}
}
