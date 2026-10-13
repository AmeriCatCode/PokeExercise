package com.example.pokedex.infrastructure.pokeapi;

import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.domain.exception.PokemonNotFoundException;
import com.example.pokedex.infrastructure.pokeapi.dto.PokemonDto;
import com.example.pokedex.infrastructure.pokeapi.dto.PokemonListDto;
import com.example.pokedex.infrastructure.pokeapi.dto.SpeciesDto;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Plain HTTP calls to PokeAPI. Translates every failure into a domain exception:
 * 404 into {@link PokemonNotFoundException}, anything else (timeout, connection error, 5xx, bad body)
 * into {@link CatalogUnavailableException}.
 */
public class PokeApiClient {

	private final RestClient restClient;

	public PokeApiClient(RestClient restClient) {
		this.restClient = restClient;
	}

	public PokemonListDto listPokemon(int offset, int limit) {
		return call(() -> restClient.get()
				.uri("/pokemon?offset={offset}&limit={limit}", offset, limit)
				.retrieve()
				.body(PokemonListDto.class));
	}

	public PokemonDto getPokemon(String idOrName) {
		return call(() -> restClient.get()
				.uri("/pokemon/{idOrName}", idOrName)
				.retrieve()
				.onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
					throw new PokemonNotFoundException(idOrName);
				})
				.body(PokemonDto.class));
	}

	public SpeciesDto getSpecies(int id) {
		return call(() -> restClient.get()
				.uri("/pokemon-species/{id}", id)
				.retrieve()
				.onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
					throw new PokemonNotFoundException("species " + id);
				})
				.body(SpeciesDto.class));
	}

	private static <T> T call(Supplier<T> request) {
		T body;
		try {
			body = request.get();
		}
		catch (RestClientException ex) {
			throw new CatalogUnavailableException("PokeAPI request failed", ex);
		}
		if (body == null) {
			throw new CatalogUnavailableException("PokeAPI returned an empty response");
		}
		return body;
	}
}
