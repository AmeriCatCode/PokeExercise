package com.example.pokedex.infrastructure.pokeapi;

import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.domain.catalog.PokemonSummary;
import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.domain.shared.PageRequest;
import com.example.pokedex.infrastructure.pokeapi.dto.PokemonDto;
import com.example.pokedex.infrastructure.pokeapi.dto.PokemonListDto;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * {@link PokemonCatalogPort} backed by PokeAPI.
 * <p>
 * PokeAPI's list only returns names, so each Pokémon of the page needs its details and its species
 * (for the category). The Pokémon of a page are loaded in parallel on virtual threads; if any of them
 * fails, the whole page fails.
 */
public class PokeApiCatalogAdapter implements PokemonCatalogPort {

	private final PokeApiClient client;
	private final PokeApiCache cache;

	public PokeApiCatalogAdapter(PokeApiClient client, PokeApiCache cache) {
		this.client = client;
		this.cache = cache;
	}

	@Override
	public Page<PokemonSummary> listPokemon(PageRequest request) {
		PokemonListDto list = client.listPokemon(request.offset(), request.size());
		List<Integer> ids = list.results().stream().map(PokeApiMapper::idFromUrl).toList();
		return new Page<>(loadInParallel(ids), request.page(), request.size(), list.count());
	}

	private List<PokemonSummary> loadInParallel(List<Integer> ids) {
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			List<Future<PokemonSummary>> futures = ids.stream()
					.map(id -> executor.submit(() -> loadSummary(id)))
					.toList();
			List<PokemonSummary> summaries = new ArrayList<>(futures.size());
			for (Future<PokemonSummary> future : futures) {
				summaries.add(await(future, executor));
			}
			return summaries;
		}
	}

	private PokemonSummary loadSummary(int id) {
		PokemonDto pokemon = cache.pokemon(String.valueOf(id));
		return PokeApiMapper.toSummary(pokemon, cache.species(PokeApiMapper.idFromUrl(pokemon.species())));
	}

	private static PokemonSummary await(Future<PokemonSummary> future, ExecutorService executor) {
		try {
			return future.get();
		}
		catch (ExecutionException ex) {
			executor.shutdownNow();
			throw ex.getCause() instanceof CatalogUnavailableException unavailable
					? unavailable
					: new CatalogUnavailableException("Could not load a Pokémon of the page", ex.getCause());
		}
		catch (InterruptedException ex) {
			executor.shutdownNow();
			Thread.currentThread().interrupt();
			throw new CatalogUnavailableException("Interrupted while loading the page", ex);
		}
	}
}
