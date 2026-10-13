package com.example.pokedex.web.catalog;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pokedex.application.usecase.GetImportStatusUseCase;
import com.example.pokedex.application.usecase.GetPokemonDetailUseCase;
import com.example.pokedex.application.usecase.ListPokemonUseCase;
import com.example.pokedex.domain.catalog.EvolutionStage;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.PokemonSummary;
import com.example.pokedex.domain.catalog.Stat;
import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.domain.exception.InvalidRequestException;
import com.example.pokedex.domain.exception.PokemonNotFoundException;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.web.error.GlobalExceptionHandler;
import com.example.pokedex.web.security.SecurityConfig;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Runs with the real security rules. Requests without a token also prove the catalog is public; with
 * {@code jwt()} they come from a logged-in user.
 */
@WebMvcTest(PokemonCatalogController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class PokemonCatalogControllerTest {

	@MockitoBean
	private JwtDecoder jwtDecoder;

	private static final PokemonSummary BULBASAUR = new PokemonSummary(1, "bulbasaur", "https://img.example/1.png",
			"Seed Pokémon", List.of("grass", "poison"), 6.9, List.of("overgrow", "chlorophyll"));

	@Autowired
	private MockMvc mockMvc;

	private static final PokemonDetail BULBASAUR_DETAIL = new PokemonDetail(1, "bulbasaur",
			"https://img.example/1.png", "https://img.example/artwork/1.png", "Seed Pokémon",
			List.of("grass", "poison"), 0.7, 6.9, List.of("overgrow", "chlorophyll"),
			List.of(new Stat("hp", 45), new Stat("attack", 49)),
			"A strange seed was planted on its back at birth.",
			List.of(new EvolutionStage(1, 1, "bulbasaur", null), new EvolutionStage(2, 2, "ivysaur", "bulbasaur")));

	@MockitoBean
	private ListPokemonUseCase listPokemon;

	@MockitoBean
	private GetPokemonDetailUseCase getPokemonDetail;

	@MockitoBean
	private GetImportStatusUseCase getImportStatus;

	@Test
	void returnsTheDetailOfAPokemon() throws Exception {
		when(getPokemonDetail.get("bulbasaur")).thenReturn(BULBASAUR_DETAIL);

		mockMvc.perform(get("/api/pokemon/bulbasaur"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("bulbasaur"))
				.andExpect(jsonPath("$.imageUrl").value("https://img.example/artwork/1.png"))
				.andExpect(jsonPath("$.spriteUrl").value("https://img.example/1.png"))
				.andExpect(jsonPath("$.category").value("Seed Pokémon"))
				.andExpect(jsonPath("$.types[1]").value("poison"))
				.andExpect(jsonPath("$.heightM").value(0.7))
				.andExpect(jsonPath("$.weightKg").value(6.9))
				.andExpect(jsonPath("$.abilities[0]").value("overgrow"))
				.andExpect(jsonPath("$.stats[0].name").value("hp"))
				.andExpect(jsonPath("$.stats[0].baseValue").value(45))
				.andExpect(jsonPath("$.description").value("A strange seed was planted on its back at birth."))
				.andExpect(jsonPath("$.evolution[1].stage").value(2))
				.andExpect(jsonPath("$.evolution[1].speciesId").value(2))
				.andExpect(jsonPath("$.evolution[1].name").value("ivysaur"))
				.andExpect(jsonPath("$.evolution[1].evolvesFrom").value("bulbasaur"));
	}

	// --- "imported" indicator (US-03) ----------------------------------------------------------------

	@Test
	void aLoggedInUserSeesThatThePokemonIsImportedAndItsLocalId() throws Exception {
		when(getPokemonDetail.get("bulbasaur")).thenReturn(BULBASAUR_DETAIL);
		when(getImportStatus.localIdOf(1)).thenReturn(Optional.of(7L));

		mockMvc.perform(get("/api/pokemon/bulbasaur").with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("bulbasaur"))
				.andExpect(jsonPath("$.imported").value(true))
				.andExpect(jsonPath("$.localId").value(7));
	}

	@Test
	void aLoggedInUserSeesThatThePokemonIsNotImported() throws Exception {
		when(getPokemonDetail.get("bulbasaur")).thenReturn(BULBASAUR_DETAIL);
		when(getImportStatus.localIdOf(1)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/pokemon/bulbasaur").with(jwt()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.imported").value(false))
				.andExpect(jsonPath("$.localId").doesNotExist());
	}

	@Test
	void aVisitorGetsNoImportInformation() throws Exception {
		when(getPokemonDetail.get("bulbasaur")).thenReturn(BULBASAUR_DETAIL);

		mockMvc.perform(get("/api/pokemon/bulbasaur"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.imported").doesNotExist())
				.andExpect(jsonPath("$.localId").doesNotExist())
				// Fields without a value stay in the response: only the import information is left out.
				.andExpect(jsonPath("$.evolution[0].evolvesFrom").value(nullValue()));
		verify(getImportStatus, never()).localIdOf(anyInt());
	}

	@Test
	void theImportStatusIsLookedUpOnEveryRequestNeverCached() throws Exception {
		when(getPokemonDetail.get("bulbasaur")).thenReturn(BULBASAUR_DETAIL);
		when(getImportStatus.localIdOf(1)).thenReturn(Optional.empty(), Optional.of(7L));

		mockMvc.perform(get("/api/pokemon/bulbasaur").with(jwt())).andExpect(jsonPath("$.imported").value(false));
		mockMvc.perform(get("/api/pokemon/bulbasaur").with(jwt())).andExpect(jsonPath("$.imported").value(true));
	}

	@Test
	void returns404ForAnUnknownPokemon() throws Exception {
		when(getPokemonDetail.get("missingno")).thenThrow(new PokemonNotFoundException("missingno"));

		mockMvc.perform(get("/api/pokemon/missingno"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("Pokémon not found"))
				.andExpect(jsonPath("$.fieldErrors").isEmpty());
	}

	@Test
	void returns400ForAnInvalidIdOrName() throws Exception {
		when(getPokemonDetail.get("pika_chu")).thenThrow(new InvalidRequestException("idOrName", "is invalid"));

		mockMvc.perform(get("/api/pokemon/pika_chu"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("idOrName"));
	}

	@Test
	void returns502ForTheDetailWhenPokeApiIsUnavailable() throws Exception {
		when(getPokemonDetail.get("1")).thenThrow(new CatalogUnavailableException("timeout"));

		mockMvc.perform(get("/api/pokemon/1"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.message").value("PokeAPI is unavailable, please try again later"));
	}

	@Test
	void returnsTheItemsAndThePageInformation() throws Exception {
		when(listPokemon.list(1, 10)).thenReturn(new Page<>(List.of(BULBASAUR), 1, 10, 1302));

		mockMvc.perform(get("/api/pokemon").param("page", "1").param("size", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").value(1))
				.andExpect(jsonPath("$.items[0].name").value("bulbasaur"))
				.andExpect(jsonPath("$.items[0].spriteUrl").value("https://img.example/1.png"))
				.andExpect(jsonPath("$.items[0].category").value("Seed Pokémon"))
				.andExpect(jsonPath("$.items[0].types[0]").value("grass"))
				.andExpect(jsonPath("$.items[0].types[1]").value("poison"))
				.andExpect(jsonPath("$.items[0].weightKg").value(6.9))
				.andExpect(jsonPath("$.items[0].abilities[0]").value("overgrow"))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(10))
				.andExpect(jsonPath("$.totalItems").value(1302))
				.andExpect(jsonPath("$.totalPages").value(131));
	}

	@Test
	void usesTheFirstPageAndASizeOf20ByDefault() throws Exception {
		when(listPokemon.list(0, 20)).thenReturn(new Page<>(List.of(BULBASAUR), 0, 20, 1302));

		mockMvc.perform(get("/api/pokemon"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(20));

		verify(listPokemon).list(0, 20);
	}

	@Test
	void rejectsANegativePage() throws Exception {
		mockMvc.perform(get("/api/pokemon").param("page", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Invalid request"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("page"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("must be greater than or equal to 0"));

		verify(listPokemon, never()).list(anyInt(), anyInt());
	}

	@Test
	void rejectsASizeAboveTheMaximum() throws Exception {
		mockMvc.perform(get("/api/pokemon").param("size", "51"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("size"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("must be less than or equal to 50"));
	}

	@Test
	void rejectsAZeroSize() throws Exception {
		mockMvc.perform(get("/api/pokemon").param("size", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
	}

	@Test
	void rejectsAPageThatIsNotANumber() throws Exception {
		mockMvc.perform(get("/api/pokemon").param("page", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("page"))
				.andExpect(jsonPath("$.fieldErrors[0].message").value("must be a whole number"));
	}

	@Test
	void returns502WithTheCommonErrorFormatWhenPokeApiIsUnavailable() throws Exception {
		when(listPokemon.list(0, 20)).thenThrow(new CatalogUnavailableException("timeout"));

		mockMvc.perform(get("/api/pokemon"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.status").value(502))
				.andExpect(jsonPath("$.message").value("PokeAPI is unavailable, please try again later"))
				.andExpect(jsonPath("$.fieldErrors").isEmpty())
				.andExpect(jsonPath("$.timestamp").isNotEmpty());
	}
}
