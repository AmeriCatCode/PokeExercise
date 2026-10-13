package com.example.pokedex.web.local;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pokedex.application.usecase.GetLocalPokemonUseCase;
import com.example.pokedex.application.usecase.ImportPokemonUseCase;
import com.example.pokedex.application.usecase.ListLocalPokemonUseCase;
import com.example.pokedex.domain.catalog.EvolutionStage;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.Stat;
import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.domain.exception.LocalPokemonNotFoundException;
import com.example.pokedex.domain.exception.PokemonAlreadyImportedException;
import com.example.pokedex.domain.exception.PokemonNotFoundException;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.web.error.GlobalExceptionHandler;
import com.example.pokedex.web.security.SecurityConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Runs with the real security rules: requests with {@code jwt()} are logged in, the others are visitors.
 */
@WebMvcTest(LocalPokemonController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class LocalPokemonControllerTest {

	private static final PokemonDetail PIKACHU = new PokemonDetail(25, "pikachu", "https://img.example/25.png",
			"https://img.example/artwork/25.png", "Mouse Pokémon", List.of("electric"), 0.4, 6.0, List.of("static"),
			List.of(new Stat("hp", 35)), "It stores electricity in its cheeks.",
			List.of(new EvolutionStage(1, 172, "pichu", null), new EvolutionStage(2, 25, "pikachu", "pichu")));

	private static final Instant CREATED = Instant.parse("2026-10-13T10:00:00Z");

	private static final LocalPokemon IMPORTED = new LocalPokemon(7L, PIKACHU, null, null, List.of(), 0L, CREATED,
			CREATED);

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JwtDecoder jwtDecoder;

	@MockitoBean
	private ImportPokemonUseCase importPokemon;

	@MockitoBean
	private ListLocalPokemonUseCase listLocalPokemon;

	@MockitoBean
	private GetLocalPokemonUseCase getLocalPokemon;

	// --- import ----------------------------------------------------------------------------------

	@Test
	void importReturns201WithTheLocalCopyAndItsLocation() throws Exception {
		when(importPokemon.importPokemon("pikachu")).thenReturn(IMPORTED);

		importAsLoggedInUser("{\"idOrName\": \"pikachu\"}")
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "http://localhost/api/local-pokemon/7"))
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.pokeapiId").value(25))
				.andExpect(jsonPath("$.name").value("pikachu"))
				.andExpect(jsonPath("$.category").value("Mouse Pokémon"))
				.andExpect(jsonPath("$.types[0]").value("electric"))
				.andExpect(jsonPath("$.stats[0].baseValue").value(35))
				.andExpect(jsonPath("$.evolution[1].evolvesFrom").value("pichu"))
				.andExpect(jsonPath("$.localName").isEmpty())
				.andExpect(jsonPath("$.localRegion").isEmpty())
				.andExpect(jsonPath("$.tags").isEmpty())
				.andExpect(jsonPath("$.version").value(0))
				.andExpect(jsonPath("$.createdAt").value("2026-10-13T10:00:00Z"));
	}

	@Test
	void importAcceptsAPokemonNumber() throws Exception {
		when(importPokemon.importPokemon("25")).thenReturn(IMPORTED);

		importAsLoggedInUser("{\"idOrName\": 25}").andExpect(status().isCreated());
	}

	@Test
	void importingAnAlreadyImportedPokemonReturns409() throws Exception {
		when(importPokemon.importPokemon("pikachu")).thenThrow(new PokemonAlreadyImportedException(25));

		importAsLoggedInUser("{\"idOrName\": \"pikachu\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("This Pokémon is already imported"));
	}

	@Test
	void importingAPokemonThatIsNotInPokeApiReturns404() throws Exception {
		when(importPokemon.importPokemon("missingno")).thenThrow(new PokemonNotFoundException("missingno"));

		importAsLoggedInUser("{\"idOrName\": \"missingno\"}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("Pokémon not found"));
	}

	@Test
	void importReturns502WhenPokeApiIsUnavailable() throws Exception {
		when(importPokemon.importPokemon("pikachu")).thenThrow(new CatalogUnavailableException("timeout"));

		importAsLoggedInUser("{\"idOrName\": \"pikachu\"}").andExpect(status().isBadGateway());
	}

	@Test
	void importWithoutIdOrNameReturns400() throws Exception {
		importAsLoggedInUser("{}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("idOrName"));
		verify(importPokemon, never()).importPokemon(anyString());
	}

	@Test
	void importWithoutLoginReturns401() throws Exception {
		mockMvc.perform(post("/api/local-pokemon").contentType(MediaType.APPLICATION_JSON)
						.content("{\"idOrName\": \"pikachu\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Authentication required"));
		verify(importPokemon, never()).importPokemon(anyString());
	}

	// --- list --------------------------------------------------------------------------------------

	@Test
	void theLocalListIsPaginated() throws Exception {
		when(listLocalPokemon.list(1, 10)).thenReturn(new Page<>(List.of(IMPORTED), 1, 10, 11));

		mockMvc.perform(get("/api/local-pokemon").param("page", "1").param("size", "10").with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").value(7))
				.andExpect(jsonPath("$.items[0].pokeapiId").value(25))
				.andExpect(jsonPath("$.items[0].name").value("pikachu"))
				.andExpect(jsonPath("$.items[0].spriteUrl").value("https://img.example/25.png"))
				.andExpect(jsonPath("$.items[0].category").value("Mouse Pokémon"))
				.andExpect(jsonPath("$.items[0].types[0]").value("electric"))
				.andExpect(jsonPath("$.items[0].tags").isEmpty())
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalItems").value(11))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void theLocalListUsesTheFirstPageAndASizeOf20ByDefault() throws Exception {
		when(listLocalPokemon.list(0, 20)).thenReturn(new Page<>(List.of(), 0, 20, 0));

		mockMvc.perform(get("/api/local-pokemon").with(jwt())).andExpect(status().isOk());

		verify(listLocalPokemon).list(0, 20);
	}

	@Test
	void theLocalListRejectsInvalidPaging() throws Exception {
		mockMvc.perform(get("/api/local-pokemon").param("size", "51").with(jwt()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
	}

	@Test
	void theLocalListRequiresLogin() throws Exception {
		mockMvc.perform(get("/api/local-pokemon")).andExpect(status().isUnauthorized());
	}

	// --- view --------------------------------------------------------------------------------------

	@Test
	void returnsOneLocalPokemon() throws Exception {
		LocalPokemon edited = new LocalPokemon(7L, PIKACHU, "Pikachu de Ash", "Kanto", List.of("starter"), 3L,
				CREATED, CREATED.plusSeconds(60));
		when(getLocalPokemon.get(7)).thenReturn(edited);

		mockMvc.perform(get("/api/local-pokemon/7").with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(7))
				.andExpect(jsonPath("$.imageUrl").value("https://img.example/artwork/25.png"))
				.andExpect(jsonPath("$.heightM").value(0.4))
				.andExpect(jsonPath("$.weightKg").value(6.0))
				.andExpect(jsonPath("$.description").value("It stores electricity in its cheeks."))
				.andExpect(jsonPath("$.localName").value("Pikachu de Ash"))
				.andExpect(jsonPath("$.localRegion").value("Kanto"))
				.andExpect(jsonPath("$.tags[0]").value("starter"))
				.andExpect(jsonPath("$.version").value(3))
				.andExpect(jsonPath("$.updatedAt").value("2026-10-13T10:01:00Z"));
	}

	@Test
	void viewingALocalPokemonThatDoesNotExistReturns404() throws Exception {
		// For example, another user deleted it.
		when(getLocalPokemon.get(99)).thenThrow(new LocalPokemonNotFoundException(99));

		mockMvc.perform(get("/api/local-pokemon/99").with(jwt()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("This Pokémon no longer exists in the local database"));
	}

	@Test
	void anIdThatIsNotANumberReturns400() throws Exception {
		mockMvc.perform(get("/api/local-pokemon/abc").with(jwt()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("id"));
	}

	@Test
	void viewingALocalPokemonRequiresLogin() throws Exception {
		mockMvc.perform(get("/api/local-pokemon/7")).andExpect(status().isUnauthorized());
	}

	private ResultActions importAsLoggedInUser(String body) throws Exception {
		return mockMvc.perform(post("/api/local-pokemon").with(jwt())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body));
	}
}
