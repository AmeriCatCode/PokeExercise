package com.example.pokedex.web.catalog;

import com.example.pokedex.application.usecase.ListPokemonUseCase;
import com.example.pokedex.domain.shared.PageRequest;
import com.example.pokedex.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public catalog of all Pokémon, read from PokeAPI.
 */
@RestController
@RequestMapping("/api/pokemon")
public class PokemonCatalogController {

	private final ListPokemonUseCase listPokemon;

	public PokemonCatalogController(ListPokemonUseCase listPokemon) {
		this.listPokemon = listPokemon;
	}

	@Operation(summary = "Browse the Pokémon catalog page by page (US-01)")
	@GetMapping
	public PageResponse<PokemonSummaryResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + PageRequest.DEFAULT_SIZE) @Min(1) @Max(PageRequest.MAX_SIZE) int size) {
		return PageResponse.from(listPokemon.list(page, size), PokemonSummaryResponse::from);
	}
}
