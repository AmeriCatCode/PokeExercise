package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.LocalPokemonRepositoryPort;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.exception.PokemonAlreadyImportedException;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.domain.shared.PageRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory {@link LocalPokemonRepositoryPort} with the same unique rule as the database. */
class FakeLocalPokemonRepository implements LocalPokemonRepositoryPort {

	private final List<LocalPokemon> pokemon = new ArrayList<>();
	private final AtomicLong ids = new AtomicLong();

	/** A minimal catalog detail, for tests that only care about the Pokémon number. */
	static PokemonDetail detailWithId(int id) {
		return new PokemonDetail(id, "pokemon-" + id, null, null, "", List.of("normal"), 1.0, 10.0, List.of(),
				List.of(), "", List.of());
	}

	@Override
	public LocalPokemon add(LocalPokemon newPokemon) {
		if (findIdByPokeapiId(newPokemon.pokeapiId()).isPresent()) {
			throw new PokemonAlreadyImportedException(newPokemon.pokeapiId());
		}
		Instant now = Instant.now();
		LocalPokemon saved = new LocalPokemon(ids.incrementAndGet(), newPokemon.pokeApiData(), newPokemon.localName(),
				newPokemon.localRegion(), newPokemon.tags(), 0L, now, now);
		pokemon.add(saved);
		return saved;
	}

	@Override
	public Optional<LocalPokemon> findById(long id) {
		return pokemon.stream().filter(p -> p.id() == id).findFirst();
	}

	@Override
	public Optional<Long> findIdByPokeapiId(int pokeapiId) {
		return pokemon.stream().filter(p -> p.pokeapiId() == pokeapiId).map(LocalPokemon::id).findFirst();
	}

	@Override
	public Page<LocalPokemon> findAll(PageRequest request) {
		List<LocalPokemon> sorted = pokemon.stream().sorted(Comparator.comparingInt(LocalPokemon::pokeapiId)).toList();
		List<LocalPokemon> items = sorted.stream().skip(request.offset()).limit(request.size()).toList();
		return new Page<>(items, request.page(), request.size(), sorted.size());
	}

	List<LocalPokemon> all() {
		return pokemon;
	}
}
