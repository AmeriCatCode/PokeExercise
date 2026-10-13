package com.example.pokedex.infrastructure.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.domain.catalog.PokemonSummary;
import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.domain.shared.PageRequest;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PokeApiCatalogAdapterTest {

	@RegisterExtension
	static WireMockExtension wm = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort().containerThreads(100))
			.build();

	private final PokeApiStubs stubs = new PokeApiStubs(wm);

	private PokeApiCatalogAdapter adapter(Duration readTimeout) {
		PokeApiProperties properties = PokeApiStubs.properties(wm, readTimeout);
		PokeApiClient client = new PokeApiClient(PokeApiConfig.restClient(properties));
		return new PokeApiCatalogAdapter(client, new PokeApiCache(client, properties.cache(), Ticker.systemTicker()));
	}

	private final PokeApiCatalogAdapter adapter = adapter(Duration.ofSeconds(2));

	@Test
	void convertsPageAndSizeIntoOffsetAndLimit() {
		stubs.list(10, 5, 1302, List.of());

		Page<PokemonSummary> page = adapter.listPokemon(new PageRequest(2, 5));

		wm.verify(getRequestedFor(urlPathEqualTo("/pokemon")));
		assertThat(page.page()).isEqualTo(2);
		assertThat(page.size()).isEqualTo(5);
		assertThat(page.totalItems()).isEqualTo(1302);
	}

	@Test
	void mapsSpriteCategoryTypesWeightAndAbilities() {
		stubs.list(0, 20, 1302, List.of(1));
		stubs.pokemon(1);
		stubs.species(1, "Seed Pokémon");

		PokemonSummary bulbasaur = adapter.listPokemon(new PageRequest(0, 20)).items().getFirst();

		assertThat(bulbasaur).isEqualTo(new PokemonSummary(1, "bulbasaur", "https://img.example/1.png",
				"Seed Pokémon", List.of("grass", "poison"), 6.9, List.of("overgrow", "chlorophyll")));
	}

	@Test
	void usesTheSpeciesUrlFromThePokemonDetails() {
		// Alternate forms have their own Pokémon id but share the species of the base form.
		stubs.list(0, 20, 1302, List.of(10033));
		stubs.pokemon(10033, 3, Duration.ZERO);
		stubs.species(3, "Seed Pokémon");

		PokemonSummary megaVenusaur = adapter.listPokemon(new PageRequest(0, 20)).items().getFirst();

		assertThat(megaVenusaur.category()).isEqualTo("Seed Pokémon");
		wm.verify(0, getRequestedFor(urlPathEqualTo("/pokemon-species/10033")));
	}

	@Test
	void keepsTheOriginalOrderOfTheItems() {
		stubs.list(0, 20, 1302, List.of(1, 3, 25));
		stubs.pokemon(1, 1, Duration.ofMillis(400));
		stubs.pokemon(3);
		stubs.pokemon(25);
		List.of(1, 3, 25).forEach(id -> stubs.species(id, "Genus " + id));

		List<Integer> ids = adapter.listPokemon(new PageRequest(0, 20)).items().stream()
				.map(PokemonSummary::id).toList();

		assertThat(ids).containsExactly(1, 3, 25);
	}

	@Test
	void fetchesThePokemonOfAPageInParallel() {
		List<Integer> ids = IntStream.rangeClosed(1, 20).boxed().toList();
		stubs.list(0, 20, 1302, ids);
		ids.forEach(id -> {
			stubs.pokemon(id, id, Duration.ofMillis(300));
			stubs.species(id, "Genus " + id, Duration.ofMillis(300));
		});

		long start = System.nanoTime();
		Page<PokemonSummary> page = adapter.listPokemon(new PageRequest(0, 20));
		Duration elapsed = Duration.ofNanos(System.nanoTime() - start);

		assertThat(page.items()).hasSize(20);
		// One after the other it would take 20 x 2 x 300 ms = 12 s.
		assertThat(elapsed).isLessThan(Duration.ofSeconds(3));
	}

	@Test
	void stopsAtTheTimeoutWhenPokeApiIsTooSlow() {
		PokeApiCatalogAdapter impatient = adapter(Duration.ofMillis(500));
		stubs.list(0, 20, 1302, List.of(1));
		stubs.pokemon(1, 1, Duration.ofSeconds(5));
		stubs.species(1, "Seed Pokémon");

		long start = System.nanoTime();
		assertThatThrownBy(() -> impatient.listPokemon(new PageRequest(0, 20)))
				.isInstanceOf(CatalogUnavailableException.class);

		assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(3));
	}

	@Test
	void failsWhenTheListFails() {
		stubs.status("/pokemon", 500);

		assertThatThrownBy(() -> adapter.listPokemon(new PageRequest(0, 20)))
				.isInstanceOf(CatalogUnavailableException.class);
	}

	@Test
	void failsWhenPokeApiIsDown() {
		wm.stubFor(get(urlPathEqualTo("/pokemon")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

		assertThatThrownBy(() -> adapter.listPokemon(new PageRequest(0, 20)))
				.isInstanceOf(CatalogUnavailableException.class);
	}

	@Test
	void failsWhenTheDetailsOfOnePokemonOfThePageFail() {
		stubs.list(0, 20, 1302, List.of(1, 3));
		stubs.pokemon(1);
		stubs.species(1, "Seed Pokémon");
		stubs.status("/pokemon/3", 500);

		assertThatThrownBy(() -> adapter.listPokemon(new PageRequest(0, 20)))
				.isInstanceOf(CatalogUnavailableException.class);
	}

	@Test
	void failsWhenTheSpeciesOfOnePokemonOfThePageFails() {
		stubs.list(0, 20, 1302, List.of(1, 3));
		stubs.pokemon(1);
		stubs.species(1, "Seed Pokémon");
		stubs.pokemon(3);
		stubs.status("/pokemon-species/3", 503);

		assertThatThrownBy(() -> adapter.listPokemon(new PageRequest(0, 20)))
				.isInstanceOf(CatalogUnavailableException.class);
	}

	@Test
	void returnsAnEmptyPageAfterTheLastPage() {
		stubs.list(2000, 20, 1302, List.of());

		Page<PokemonSummary> page = adapter.listPokemon(new PageRequest(100, 20));

		assertThat(page.items()).isEmpty();
		assertThat(page.totalPages()).isEqualTo(66);
	}
}
