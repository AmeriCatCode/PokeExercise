package com.example.pokedex.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.TestcontainersConfiguration;
import com.example.pokedex.domain.catalog.EvolutionStage;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.Stat;
import com.example.pokedex.domain.exception.PokemonAlreadyImportedException;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.domain.shared.PageRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs against a real PostgreSQL, because the unique rule that matters here lives in the database.
 * No test-wide transaction: each save commits on its own, as in production.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaLocalPokemonRepositoryAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaLocalPokemonRepositoryAdapterTest {

	private static final PokemonDetail EEVEE = new PokemonDetail(133, "eevee", "https://img.example/133.png",
			"https://img.example/artwork/133.png", "Evolution Pokémon", List.of("normal"), 0.3, 6.5,
			List.of("run-away", "adaptability", "anticipation"),
			List.of(new Stat("hp", 55), new Stat("attack", 55), new Stat("speed", 55)),
			"Its genetic code is irregular.",
			List.of(new EvolutionStage(1, 133, "eevee", null), new EvolutionStage(2, 134, "vaporeon", "eevee"),
					new EvolutionStage(2, 135, "jolteon", "eevee")));

	@Autowired
	private JpaLocalPokemonRepositoryAdapter repository;

	@Autowired
	private PokemonJpaRepository jpa;

	@AfterEach
	void cleanUp() {
		jpa.deleteAll();
	}

	@Test
	void savesANewImportWithVersionAndTimestamps() {
		LocalPokemon saved = repository.add(LocalPokemon.importFrom(EEVEE));

		assertThat(saved.id()).isNotNull();
		assertThat(saved.version()).isZero();
		assertThat(saved.createdAt()).isNotNull();
		assertThat(saved.updatedAt()).isEqualTo(saved.createdAt());
		assertThat(saved.localName()).isNull();
		assertThat(saved.tags()).isEmpty();
	}

	@Test
	void readsBackThePokeApiDataExactlyAsSavedWithItsTags() {
		LocalPokemon withTags = new LocalPokemon(null, EEVEE, "Évoli", "Kanto", List.of("starter", "fan-favorite"),
				null, null, null);
		LocalPokemon saved = repository.add(withTags);

		LocalPokemon found = repository.findById(saved.id()).orElseThrow();

		assertThat(found.pokeApiData()).isEqualTo(EEVEE);
		assertThat(found.localName()).isEqualTo("Évoli");
		assertThat(found.localRegion()).isEqualTo("Kanto");
		assertThat(found.tags()).containsExactlyInAnyOrder("starter", "fan-favorite");
		assertThat(found.version()).isZero();
		assertThat(found.createdAt()).isEqualTo(saved.createdAt());
	}

	@Test
	void findsNothingForAnUnknownId() {
		assertThat(repository.findById(999_999)).isEmpty();
	}

	@Test
	void findsTheLocalIdByPokeApiId() {
		LocalPokemon saved = repository.add(LocalPokemon.importFrom(EEVEE));

		assertThat(repository.findIdByPokeapiId(133)).contains(saved.id());
		assertThat(repository.findIdByPokeapiId(1)).isEmpty();
	}

	@Test
	void theDatabaseRejectsADuplicatePokeApiId() {
		repository.add(LocalPokemon.importFrom(EEVEE));

		assertThatThrownBy(() -> repository.add(LocalPokemon.importFrom(EEVEE)))
				.isInstanceOf(PokemonAlreadyImportedException.class)
				.extracting("pokeapiId").isEqualTo(133);
		assertThat(jpa.count()).isEqualTo(1);
	}

	@Test
	void listsPageByPageInPokeApiOrder() {
		for (int id : new int[] {25, 1, 133, 4}) {
			repository.add(LocalPokemon.importFrom(withId(id)));
		}

		Page<LocalPokemon> first = repository.findAll(new PageRequest(0, 3));
		Page<LocalPokemon> second = repository.findAll(new PageRequest(1, 3));

		assertThat(first.items()).extracting(LocalPokemon::pokeapiId).containsExactly(1, 4, 25);
		assertThat(second.items()).extracting(LocalPokemon::pokeapiId).containsExactly(133);
		assertThat(first.totalItems()).isEqualTo(4);
		assertThat(first.totalPages()).isEqualTo(2);
		assertThat(second.page()).isEqualTo(1);
		// The list items carry their child data too.
		assertThat(first.items().getFirst().pokeApiData().types()).containsExactly("normal");
	}

	@Test
	void simultaneousImportsOfTheSamePokemonSaveExactlyOne() throws Exception {
		int attempts = 5;
		CountDownLatch start = new CountDownLatch(1);
		List<Future<LocalPokemon>> results = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(attempts)) {
			for (int i = 0; i < attempts; i++) {
				Callable<LocalPokemon> importEevee = () -> {
					start.await();
					return repository.add(LocalPokemon.importFrom(EEVEE));
				};
				results.add(executor.submit(importEevee));
			}
			start.countDown();

			int saved = 0;
			int rejected = 0;
			for (Future<LocalPokemon> result : results) {
				try {
					result.get();
					saved++;
				}
				catch (ExecutionException ex) {
					// "Already imported", never a generic database error.
					assertThat(ex.getCause()).isInstanceOf(PokemonAlreadyImportedException.class);
					rejected++;
				}
			}
			assertThat(saved).isEqualTo(1);
			assertThat(rejected).isEqualTo(attempts - 1);
		}
		assertThat(jpa.count()).isEqualTo(1);
	}

	private static PokemonDetail withId(int id) {
		return new PokemonDetail(id, "pokemon-" + id, null, null, "", List.of("normal"), 1.0, 10.0, List.of(),
				List.of(), "", List.of());
	}
}
