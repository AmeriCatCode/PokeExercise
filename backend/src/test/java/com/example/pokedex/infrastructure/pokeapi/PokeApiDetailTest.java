package com.example.pokedex.infrastructure.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.domain.catalog.EvolutionStage;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.Stat;
import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.domain.exception.PokemonNotFoundException;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * US-02: the detail combines the Pokémon, its species and its evolution chain.
 */
class PokeApiDetailTest {

	@RegisterExtension
	static WireMockExtension wm = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	private final PokeApiStubs stubs = new PokeApiStubs(wm);

	private PokeApiCatalogAdapter adapter(Duration readTimeout) {
		PokeApiProperties properties = PokeApiStubs.properties(wm, readTimeout);
		PokeApiClient client = new PokeApiClient(PokeApiConfig.restClient(properties));
		return new PokeApiCatalogAdapter(client, new PokeApiCache(client, properties.cache(), Ticker.systemTicker()));
	}

	private final PokeApiCatalogAdapter adapter = adapter(Duration.ofSeconds(2));

	@Test
	void combinesThePokemonItsSpeciesAndItsEvolutionChain() {
		stubs.bulbasaurDetail();

		PokemonDetail detail = adapter.getPokemonDetail("1");

		assertThat(detail).isEqualTo(new PokemonDetail(
				1,
				"bulbasaur",
				"https://img.example/1.png",
				"https://img.example/artwork/1.png",
				"Seed Pokémon",
				List.of("grass", "poison"),
				0.7,
				6.9,
				List.of("overgrow", "chlorophyll"),
				List.of(new Stat("hp", 45), new Stat("attack", 49), new Stat("defense", 49),
						new Stat("special-attack", 65), new Stat("special-defense", 65), new Stat("speed", 45)),
				"A strange seed was planted on its back at birth.",
				List.of(new EvolutionStage(1, 1, "bulbasaur", null),
						new EvolutionStage(2, 2, "ivysaur", "bulbasaur"),
						new EvolutionStage(3, 3, "venusaur", "ivysaur"))));
	}

	@Test
	void acceptsTheNameInAnyCase() {
		stubs.bulbasaurDetail();

		assertThat(adapter.getPokemonDetail("Bulbasaur").id()).isEqualTo(1);
	}

	@Test
	void anUnknownPokemonIsNotFound() {
		stubs.status("/pokemon/missingno", 404);

		assertThatThrownBy(() -> adapter.getPokemonDetail("missingno")).isInstanceOf(PokemonNotFoundException.class);
	}

	@Test
	void failsWhenPokeApiIsTooSlow() {
		PokeApiCatalogAdapter impatient = adapter(Duration.ofMillis(300));
		stubs.pokemon(1, 1, Duration.ofSeconds(3));

		assertThatThrownBy(() -> impatient.getPokemonDetail("1")).isInstanceOf(CatalogUnavailableException.class);
	}

	@Test
	void failsWhenTheSpeciesCannotBeLoaded() {
		stubs.pokemon(1);
		stubs.status("/pokemon-species/1", 500);

		assertThatThrownBy(() -> adapter.getPokemonDetail("1")).isInstanceOf(CatalogUnavailableException.class);
	}

	@Test
	void failsWhenTheEvolutionChainCannotBeLoaded() {
		stubs.pokemon(1);
		stubs.species(1, "Seed Pokémon");
		stubs.status("/evolution-chain/1", 503);

		assertThatThrownBy(() -> adapter.getPokemonDetail("1")).isInstanceOf(CatalogUnavailableException.class);
	}

	@Test
	void aSecondRequestDoesNotCallPokeApiAgain() {
		stubs.bulbasaurDetail();

		adapter.getPokemonDetail("1");
		adapter.getPokemonDetail("1");

		wm.verify(1, getRequestedFor(urlPathEqualTo("/pokemon/1")));
		wm.verify(1, getRequestedFor(urlPathEqualTo("/pokemon-species/1")));
		wm.verify(1, getRequestedFor(urlPathEqualTo("/evolution-chain/1")));
	}

	@Test
	void nameAndNumberShareTheSameCacheEntry() {
		stubs.bulbasaurDetail();

		adapter.getPokemonDetail("bulbasaur");
		adapter.getPokemonDetail("1");

		wm.verify(1, getRequestedFor(urlPathEqualTo("/pokemon/bulbasaur")));
		wm.verify(0, getRequestedFor(urlPathEqualTo("/pokemon/1")));
		wm.verify(1, getRequestedFor(urlPathEqualTo("/pokemon-species/1")));
		wm.verify(1, getRequestedFor(urlPathEqualTo("/evolution-chain/1")));
	}

	@Test
	void aFailureIsNotCached() {
		stubs.pokemon(1);
		stubs.species(1, "Seed Pokémon");
		stubs.status("/evolution-chain/1", 503);
		assertThatThrownBy(() -> adapter.getPokemonDetail("1")).isInstanceOf(CatalogUnavailableException.class);

		stubs.evolutionChain();

		assertThat(adapter.getPokemonDetail("1").evolution()).hasSize(3);
	}
}
