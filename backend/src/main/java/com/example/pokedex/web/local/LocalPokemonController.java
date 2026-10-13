package com.example.pokedex.web.local;

import com.example.pokedex.application.usecase.DeleteLocalPokemonUseCase;
import com.example.pokedex.application.usecase.GetLocalPokemonUseCase;
import com.example.pokedex.application.usecase.ImportPokemonUseCase;
import com.example.pokedex.application.usecase.ListLocalPokemonUseCase;
import com.example.pokedex.application.usecase.UpdateLocalPokemonUseCase;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.shared.PageRequest;
import com.example.pokedex.web.PageResponse;
import com.example.pokedex.web.local.LocalPokemonDtos.ImportRequest;
import com.example.pokedex.web.local.LocalPokemonDtos.LocalPokemonResponse;
import com.example.pokedex.web.local.LocalPokemonDtos.LocalPokemonSummaryResponse;
import com.example.pokedex.web.local.LocalPokemonDtos.UpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
	private final UpdateLocalPokemonUseCase updateLocalPokemon;
	private final DeleteLocalPokemonUseCase deleteLocalPokemon;

	public LocalPokemonController(ImportPokemonUseCase importPokemon, ListLocalPokemonUseCase listLocalPokemon,
			GetLocalPokemonUseCase getLocalPokemon, UpdateLocalPokemonUseCase updateLocalPokemon,
			DeleteLocalPokemonUseCase deleteLocalPokemon) {
		this.importPokemon = importPokemon;
		this.listLocalPokemon = listLocalPokemon;
		this.getLocalPokemon = getLocalPokemon;
		this.updateLocalPokemon = updateLocalPokemon;
		this.deleteLocalPokemon = deleteLocalPokemon;
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

	@Operation(summary = "Change the local name, local region and tags, sending the version you loaded (US-04)")
	@PutMapping("/{id}")
	public LocalPokemonResponse update(@PathVariable long id, @Valid @RequestBody UpdateRequest request) {
		request.rejectOtherFields();
		return LocalPokemonResponse.from(updateLocalPokemon.update(id, request.version(), request.localName(),
				request.localRegion(), request.tags()));
	}

	@Operation(summary = "Delete a local Pokémon, sending the version you loaded (US-04)")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable long id, @RequestParam long version) {
		deleteLocalPokemon.delete(id, version);
	}
}
