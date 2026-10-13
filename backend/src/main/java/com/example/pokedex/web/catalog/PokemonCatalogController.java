package com.example.pokedex.web.catalog;

import com.example.pokedex.application.usecase.GetImportStatusUseCase;
import com.example.pokedex.application.usecase.GetPokemonDetailUseCase;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.application.usecase.ListPokemonUseCase;
import com.example.pokedex.domain.shared.PageRequest;
import com.example.pokedex.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public catalog of all Pokémon, read from PokeAPI. A logged-in user also sees, on the detail, whether
 * each Pokémon is already imported.
 */
@RestController
@RequestMapping("/api/pokemon")
public class PokemonCatalogController {

	private final ListPokemonUseCase listPokemon;
	private final GetPokemonDetailUseCase getPokemonDetail;
	private final GetImportStatusUseCase getImportStatus;

	public PokemonCatalogController(ListPokemonUseCase listPokemon, GetPokemonDetailUseCase getPokemonDetail,
			GetImportStatusUseCase getImportStatus) {
		this.listPokemon = listPokemon;
		this.getPokemonDetail = getPokemonDetail;
		this.getImportStatus = getImportStatus;
	}

	@Operation(summary = "Browse the Pokémon catalog page by page (US-01)")
	@GetMapping
	public PageResponse<PokemonSummaryResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + PageRequest.DEFAULT_SIZE) @Min(1) @Max(PageRequest.MAX_SIZE) int size) {
		return PageResponse.from(listPokemon.list(page, size), PokemonSummaryResponse::from);
	}

	@Operation(summary = "See the detail of one Pokémon, by number or name (US-02); with a token, its import status")
	@GetMapping("/{idOrName}")
	public PokemonDetailResponse detail(@PathVariable String idOrName, @AuthenticationPrincipal Jwt user) {
		PokemonDetail detail = getPokemonDetail.get(idOrName);
		if (user == null) {
			return PokemonDetailResponse.from(detail);
		}
		// The PokeAPI data may come from the cache, but the import status is always read fresh from the
		// database, so a Pokémon shows as imported right after anyone imports it.
		return PokemonDetailResponse.from(detail, getImportStatus.localIdOf(detail.id()));
	}
}
