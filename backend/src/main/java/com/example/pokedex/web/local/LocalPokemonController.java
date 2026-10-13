package com.example.pokedex.web.local;

import com.example.pokedex.application.usecase.GetLocalPokemonUseCase;
import com.example.pokedex.application.usecase.ImportPokemonUseCase;
import com.example.pokedex.application.usecase.ListLocalPokemonUseCase;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.shared.PageRequest;
import com.example.pokedex.web.PageResponse;
import com.example.pokedex.web.local.LocalPokemonDtos.ImportRequest;
import com.example.pokedex.web.local.LocalPokemonDtos.LocalPokemonResponse;
import com.example.pokedex.web.local.LocalPokemonDtos.LocalPokemonSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The Pokémon imported into the local database (protected: every route needs a login token).
 */
@RestController
@RequestMapping("/api/local-pokemon")
public class LocalPokemonController {

	private final ImportPokemonUseCase importPokemon;
	private final ListLocalPokemonUseCase listLocalPokemon;
	private final GetLocalPokemonUseCase getLocalPokemon;

	public LocalPokemonController(ImportPokemonUseCase importPokemon, ListLocalPokemonUseCase listLocalPokemon,
			GetLocalPokemonUseCase getLocalPokemon) {
		this.importPokemon = importPokemon;
		this.listLocalPokemon = listLocalPokemon;
		this.getLocalPokemon = getLocalPokemon;
	}

	@Operation(summary = "Import a Pokémon from PokeAPI into the local database (US-03)")
	@PostMapping
	public ResponseEntity<LocalPokemonResponse> importPokemon(@Valid @RequestBody ImportRequest request) {
		LocalPokemon imported = importPokemon.importPokemon(request.idOrName());
		var location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
				.buildAndExpand(imported.id()).toUri();
		return ResponseEntity.created(location).body(LocalPokemonResponse.from(imported));
	}

	@Operation(summary = "Browse the local Pokémon page by page, in Pokédex order")
	@GetMapping
	public PageResponse<LocalPokemonSummaryResponse> list(
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "" + PageRequest.DEFAULT_SIZE) @Min(1) @Max(PageRequest.MAX_SIZE) int size) {
		return PageResponse.from(listLocalPokemon.list(page, size), LocalPokemonSummaryResponse::from);
	}

	@Operation(summary = "View one local Pokémon")
	@GetMapping("/{id}")
	public LocalPokemonResponse get(@PathVariable long id) {
		return LocalPokemonResponse.from(getLocalPokemon.get(id));
	}
}
