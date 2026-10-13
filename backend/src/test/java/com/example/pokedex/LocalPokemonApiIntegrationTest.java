package com.example.pokedex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.domain.catalog.EvolutionStage;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.Stat;
import com.example.pokedex.domain.exception.PokemonNotFoundException;
import com.example.pokedex.domain.user.Role;
import com.example.pokedex.domain.user.User;
import com.example.pokedex.infrastructure.security.JwtKeys;
import com.example.pokedex.infrastructure.security.JwtTokenIssuer;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Import, local list/view, update, delete and the "imported" indicator through the whole application:
 * HTTP → security → controllers → use cases → PostgreSQL (Testcontainers). Only PokeAPI is faked.
 */
@SpringBootTest(properties = "security.jwt.secret=" + LocalPokemonApiIntegrationTest.SECRET)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class LocalPokemonApiIntegrationTest {

	static final String SECRET = "dGVzdC1zZWNyZXQtZm9yLXRoZS1wb2tlZGV4LWFwaS10ZXN0cw==";

	private static final PokemonDetail PIKACHU = new PokemonDetail(25, "pikachu", "https://img.example/25.png",
			"https://img.example/artwork/25.png", "Mouse Pokémon", List.of("electric"), 0.4, 6.0, List.of("static"),
			List.of(new Stat("hp", 35)), "It stores electricity in its cheeks.",
			List.of(new EvolutionStage(1, 172, "pichu", null), new EvolutionStage(2, 25, "pikachu", "pichu")));

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	@MockitoBean
	private PokemonCatalogPort catalog;

	private final String token = new JwtTokenIssuer(JwtKeys.encoder(JwtKeys.fromBase64(SECRET)),
			Duration.ofHours(1), Clock.systemUTC())
			.issue(new User(1L, "ash", "ash@example.com", "hash", Role.USER)).value();

	@AfterEach
	void deletePokemon() {
		jdbc.update("DELETE FROM pokemon");
	}

	@Test
	void importsAPokemonAndShowsItInTheLocalListAndView() throws Exception {
		when(catalog.getPokemonDetail("pikachu")).thenReturn(PIKACHU);

		String body = importPokemon("pikachu")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.pokeapiId").value(25))
				.andExpect(jsonPath("$.localName").isEmpty())
				.andReturn().getResponse().getContentAsString();
		int localId = JsonPath.read(body, "$.id");

		withToken(get("/api/local-pokemon"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalItems").value(1))
				.andExpect(jsonPath("$.items[0].id").value(localId))
				.andExpect(jsonPath("$.items[0].name").value("pikachu"));
		withToken(get("/api/local-pokemon/" + localId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.category").value("Mouse Pokémon"))
				.andExpect(jsonPath("$.stats[0].name").value("hp"))
				.andExpect(jsonPath("$.evolution[0].name").value("pichu"))
				.andExpect(jsonPath("$.version").value(0));
	}

	@Test
	void pokeApiIsNeverCalledInsideADatabaseTransaction() throws Exception {
		List<Boolean> transactionActive = new CopyOnWriteArrayList<>();
		when(catalog.getPokemonDetail("pikachu")).thenAnswer(call -> {
			transactionActive.add(TransactionSynchronizationManager.isActualTransactionActive());
			return PIKACHU;
		});

		importPokemon("pikachu").andExpect(status().isCreated());

		assertThat(transactionActive).containsExactly(false);
	}

	@Test
	void aSecondImportOfTheSamePokemonReturns409() throws Exception {
		when(catalog.getPokemonDetail(anyString())).thenReturn(PIKACHU);
		importPokemon("pikachu").andExpect(status().isCreated());

		importPokemon("25")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("This Pokémon is already imported"));
	}

	@Test
	void simultaneousImportsOfTheSamePokemonGiveOne201AndOnly409sNever500() throws Exception {
		int attempts = 4;
		// Every import waits here after PokeAPI answered, so all of them pass the "already imported?"
		// check together and race to insert: only the database's unique constraint can stop them.
		CyclicBarrier allFetched = new CyclicBarrier(attempts);
		when(catalog.getPokemonDetail("pikachu")).thenAnswer(call -> {
			allFetched.await(10, TimeUnit.SECONDS);
			return PIKACHU;
		});

		List<Integer> statuses = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(attempts)) {
			List<Future<Integer>> results = new ArrayList<>();
			for (int i = 0; i < attempts; i++) {
				results.add(executor.submit(() -> importPokemon("pikachu").andReturn().getResponse().getStatus()));
			}
			for (Future<Integer> result : results) {
				statuses.add(result.get(30, TimeUnit.SECONDS));
			}
		}

		assertThat(statuses).containsOnly(201, 409).filteredOn(s -> s == 201).hasSize(1);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM pokemon", Integer.class)).isEqualTo(1);
	}

	@Test
	void importingAPokemonThatIsNotInPokeApiReturns404() throws Exception {
		when(catalog.getPokemonDetail("missingno")).thenThrow(new PokemonNotFoundException("missingno"));

		importPokemon("missingno").andExpect(status().isNotFound());
	}

	@Test
	void viewingALocalPokemonThatWasDeletedReturns404() throws Exception {
		withToken(get("/api/local-pokemon/123456"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("This Pokémon no longer exists in the local database"));
	}

	@Test
	void everyLocalRouteRequiresLogin() throws Exception {
		mockMvc.perform(post("/api/local-pokemon").contentType(MediaType.APPLICATION_JSON)
				.content("{\"idOrName\": \"pikachu\"}")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/local-pokemon")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/local-pokemon/1")).andExpect(status().isUnauthorized());
	}

	// --- "imported" indicator on the catalog detail ---------------------------------------------------

	@Test
	void rightAfterAnImportTheDetailShowsItAsImported() throws Exception {
		// The catalog answers with the very same object every time, as the PokeAPI cache does: the
		// indicator can't come from there.
		when(catalog.getPokemonDetail(anyString())).thenReturn(PIKACHU);
		withToken(get("/api/pokemon/pikachu")).andExpect(jsonPath("$.imported").value(false));

		String body = importPokemon("pikachu").andReturn().getResponse().getContentAsString();
		int localId = JsonPath.read(body, "$.id");

		withToken(get("/api/pokemon/pikachu"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.imported").value(true))
				.andExpect(jsonPath("$.localId").value(localId));
	}

	@Test
	void visitorsGetTheDetailWithoutImportInformation() throws Exception {
		when(catalog.getPokemonDetail("pikachu")).thenReturn(PIKACHU);
		importPokemon("pikachu");

		mockMvc.perform(get("/api/pokemon/pikachu"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("pikachu"))
				.andExpect(jsonPath("$.imported").doesNotExist())
				.andExpect(jsonPath("$.localId").doesNotExist());
	}

	@Test
	void anInvalidTokenOnTheDetailIsAnExpiredSessionNotAVisitor() throws Exception {
		// The client only sends a token when it believes it is logged in; answering as if it were a
		// visitor would silently hide the import status, so the client is told to log in again.
		when(catalog.getPokemonDetail("pikachu")).thenReturn(PIKACHU);

		mockMvc.perform(get("/api/pokemon/pikachu").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Your session has expired or is invalid, please log in again"));
	}

	// --- update and delete: the concurrency scenarios of US-04 --------------------------------------

	@Test
	void anUpdateSavesTheProprietaryFieldsAndReturnsTheNewVersion() throws Exception {
		int id = importedPikachu();

		update(id, "Pikachu de Ash", "[\"Starter\", \"electric\"]", 0)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.localName").value("Pikachu de Ash"))
				.andExpect(jsonPath("$.tags", containsInAnyOrder("starter", "electric")))
				.andExpect(jsonPath("$.version").value(1));
		withToken(get("/api/local-pokemon/" + id))
				.andExpect(jsonPath("$.localName").value("Pikachu de Ash"))
				.andExpect(jsonPath("$.name").value("pikachu"))
				.andExpect(jsonPath("$.version").value(1));
	}

	@Test
	void twoUsersEditingTheSamePokemonTheSecondSaveIsRejected() throws Exception {
		// C1: Ana and Ben both loaded version 0. Ana saves first.
		int id = importedPikachu();
		update(id, "Ana's name", "[]", 0).andExpect(status().isOk());

		update(id, "Ben's name", "[]", 0)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("This Pokémon was changed by someone else, please reload it"));
		withToken(get("/api/local-pokemon/" + id)).andExpect(jsonPath("$.localName").value("Ana's name"));
	}

	@Test
	void aTagsOnlyChangeIncreasesTheVersionSoAnOldVersionIsRejected() throws Exception {
		int id = importedPikachu();
		update(id, null, "[\"starter\"]", 0).andExpect(jsonPath("$.version").value(1));

		update(id, null, "[\"starter\", \"favorite\"]", 1).andExpect(jsonPath("$.version").value(2));
		update(id, null, "[]", 1).andExpect(status().isConflict());
	}

	@Test
	void deletingAPokemonSomeoneElseChangedIsRejected() throws Exception {
		// C2: Ana saves a change; Ben, still on version 0, deletes.
		int id = importedPikachu();
		update(id, "Ana's name", "[]", 0).andExpect(status().isOk());

		withToken(delete("/api/local-pokemon/" + id).param("version", "0")).andExpect(status().isConflict());
		withToken(get("/api/local-pokemon/" + id)).andExpect(status().isOk());
	}

	@Test
	void editingAPokemonThatWasJustDeletedReturns404() throws Exception {
		// C3
		int id = importedPikachu();
		withToken(delete("/api/local-pokemon/" + id).param("version", "0")).andExpect(status().isNoContent());

		update(id, "Too late", "[]", 0).andExpect(status().isNotFound());
	}

	@Test
	void twoUsersDeletingTheSamePokemonTheSecondGets404() throws Exception {
		// C4
		int id = importedPikachu();

		withToken(delete("/api/local-pokemon/" + id).param("version", "0")).andExpect(status().isNoContent());
		withToken(delete("/api/local-pokemon/" + id).param("version", "0")).andExpect(status().isNotFound());
	}

	@Test
	void afterADeleteTheCatalogDetailShowsItAsNotImportedAndItCanBeImportedAgain() throws Exception {
		int id = importedPikachu();

		withToken(delete("/api/local-pokemon/" + id).param("version", "0")).andExpect(status().isNoContent());

		withToken(get("/api/pokemon/pikachu")).andExpect(jsonPath("$.imported").value(false));
		importPokemon("pikachu").andExpect(status().isCreated());
	}

	@Test
	void readOnlyFieldsCanNotBeChanged() throws Exception {
		int id = importedPikachu();

		withToken(put("/api/local-pokemon/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"tags\": [], \"version\": 0, \"category\": \"Rat Pokémon\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("category"));
		withToken(get("/api/local-pokemon/" + id))
				.andExpect(jsonPath("$.category").value("Mouse Pokémon"))
				.andExpect(jsonPath("$.version").value(0));
	}

	@Test
	void updateAndDeleteRequireLogin() throws Exception {
		mockMvc.perform(put("/api/local-pokemon/1").contentType(MediaType.APPLICATION_JSON)
				.content("{\"tags\": [], \"version\": 0}")).andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/local-pokemon/1").param("version", "0")).andExpect(status().isUnauthorized());
	}

	// --- helpers ----------------------------------------------------------------------------------

	private int importedPikachu() throws Exception {
		when(catalog.getPokemonDetail(anyString())).thenReturn(PIKACHU);
		String body = importPokemon("pikachu").andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	private ResultActions update(int id, String localName, String tagsJson, long version) throws Exception {
		String name = localName == null ? "null" : "\"" + localName + "\"";
		return withToken(put("/api/local-pokemon/" + id).contentType(MediaType.APPLICATION_JSON)
				.content("{\"localName\": %s, \"tags\": %s, \"version\": %d}".formatted(name, tagsJson, version)));
	}

	private ResultActions importPokemon(String idOrName) throws Exception {
		return withToken(post("/api/local-pokemon")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"idOrName\": \"%s\"}".formatted(idOrName)));
	}

	private ResultActions withToken(
			org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
		return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
	}
}
