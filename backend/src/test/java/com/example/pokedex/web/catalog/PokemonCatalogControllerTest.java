package com.example.pokedex.web.catalog;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pokedex.application.usecase.ListPokemonUseCase;
import com.example.pokedex.domain.catalog.PokemonSummary;
import com.example.pokedex.domain.exception.CatalogUnavailableException;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.web.error.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PokemonCatalogController.class)
@Import(GlobalExceptionHandler.class)
class PokemonCatalogControllerTest {

	private static final PokemonSummary BULBASAUR = new PokemonSummary(1, "bulbasaur", "https://img.example/1.png",
			"Seed Pokémon", List.of("grass", "poison"), 6.9, List.of("overgrow", "chlorophyll"));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ListPokemonUseCase listPokemon;

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
