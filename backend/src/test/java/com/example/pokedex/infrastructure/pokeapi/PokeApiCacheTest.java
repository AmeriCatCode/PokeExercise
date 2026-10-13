package com.example.pokedex.infrastructure.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.domain.exception.PokemonNotFoundException;
import com.github.benmanes.caffeine.cache.Ticker;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class PokeApiCacheTest {

	@RegisterExtension
	static WireMockExtension wm = WireMockExtension.newInstance()
			.options(wireMockConfig().dynamicPort())
			.build();

	private final PokeApiStubs stubs = new PokeApiStubs(wm);
	private final FakeTicker ticker = new FakeTicker();
	private final PokeApiProperties properties = PokeApiStubs.properties(wm, Duration.ofSeconds(2));
	private final PokeApiCache cache = new PokeApiCache(
			new PokeApiClient(PokeApiConfig.restClient(properties)), properties.cache(), ticker);

	@Test
	void aSecondRequestForTheSamePokemonDoesNotCallPokeApiAgain() {
		stubs.pokemon(1);
		stubs.species(1, "Seed Pokémon");

		cache.pokemon("1");
		cache.pokemon("1");
		cache.species(1);
		cache.species(1);

		wm.verify(1, getRequestedFor(urlPathEqualTo("/pokemon/1")));
		wm.verify(1, getRequestedFor(urlPathEqualTo("/pokemon-species/1")));
	}

	@Test
	void nameInAnyCaseAndNumberShareTheSameEntry() {
		stubs.pokemonByName("pikachu", 25);
		stubs.pokemon(25);

		assertThat(cache.pokemon("Pikachu").id()).isEqualTo(25);
		assertThat(cache.pokemon("pikachu").id()).isEqualTo(25);
		assertThat(cache.pokemon("25").id()).isEqualTo(25);

		wm.verify(1, getRequestedFor(urlPathEqualTo("/pokemon/pikachu")));
		wm.verify(0, getRequestedFor(urlPathEqualTo("/pokemon/25")));
	}

	@Test
	void numberThenNameShareTheSameEntry() {
		stubs.pokemon(25);
		stubs.pokemonByName("pikachu", 25);

		cache.pokemon("25");
		cache.pokemon("PIKACHU");

		wm.verify(1, getRequestedFor(urlPathEqualTo("/pokemon/25")));
		wm.verify(0, getRequestedFor(urlPathEqualTo("/pokemon/pikachu")));
	}

	@Test
	void aFailedCallIsNotCached() {
		stubs.status("/pokemon/1", 500);
		assertThatThrownBy(() -> cache.pokemon("1")).isInstanceOf(CatalogUnavailableException.class);

		stubs.pokemon(1);

		assertThat(cache.pokemon("1").id()).isEqualTo(1);
		wm.verify(2, getRequestedFor(urlPathEqualTo("/pokemon/1")));
	}

	@Test
	void aNotFoundIsNotCached() {
		stubs.status("/pokemon/missingno", 404);
		assertThatThrownBy(() -> cache.pokemon("missingno")).isInstanceOf(PokemonNotFoundException.class);
		assertThatThrownBy(() -> cache.pokemon("missingno")).isInstanceOf(PokemonNotFoundException.class);

		wm.verify(2, getRequestedFor(urlPathEqualTo("/pokemon/missingno")));
	}

	@Test
	void entriesExpireAfterTheConfiguredTime() {
		stubs.pokemon(1);
		cache.pokemon("1");

		ticker.advance(Duration.ofHours(24).plusSeconds(1));
		cache.pokemon("1");

		wm.verify(2, getRequestedFor(urlPathEqualTo("/pokemon/1")));
	}

	@Test
	void simultaneousRequestsForAnUncachedPokemonMakeOnlyOneCall() throws Exception {
		stubs.pokemon(1, 1, Duration.ofMillis(500));
		CountDownLatch start = new CountDownLatch(1);

		CompletableFuture<?> first = CompletableFuture.runAsync(() -> awaitThen(start, () -> cache.pokemon("1")));
		CompletableFuture<?> second = CompletableFuture.runAsync(() -> awaitThen(start, () -> cache.pokemon("1")));
		start.countDown();
		CompletableFuture.allOf(first, second).get();

		wm.verify(1, getRequestedFor(urlPathEqualTo("/pokemon/1")));
	}

	private static void awaitThen(CountDownLatch latch, Runnable action) {
		try {
			latch.await();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
		action.run();
	}

	private static final class FakeTicker implements Ticker {

		private final AtomicLong nanos = new AtomicLong();

		@Override
		public long read() {
			return nanos.get();
		}

		void advance(Duration duration) {
			nanos.addAndGet(duration.toNanos());
		}
	}
}
