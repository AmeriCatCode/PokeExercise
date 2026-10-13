package com.example.pokedex.infrastructure.persistence;

import com.example.pokedex.domain.catalog.EvolutionStage;
import com.example.pokedex.domain.catalog.PokemonDetail;
import com.example.pokedex.domain.catalog.Stat;
import com.example.pokedex.domain.local.LocalPokemon;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An imported Pokémon. The PokeAPI columns are written once, at import ({@code updatable = false});
 * only the proprietary fields can change, and every change increments {@link #version}.
 */
@Entity
@Table(name = "pokemon")
class PokemonEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "pokeapi_id", nullable = false, updatable = false)
	private int pokeapiId;

	// --- copied from PokeAPI ---------------------------------------------------------------------

	@Column(nullable = false, updatable = false, length = 100)
	private String name;

	@Column(name = "sprite_url", updatable = false, length = 500)
	private String spriteUrl;

	@Column(name = "image_url", updatable = false, length = 500)
	private String imageUrl;

	@Column(nullable = false, updatable = false, length = 100)
	private String category;

	@Column(name = "height_m", nullable = false, updatable = false)
	private double heightM;

	@Column(name = "weight_kg", nullable = false, updatable = false)
	private double weightKg;

	@Column(nullable = false, updatable = false, columnDefinition = "text")
	private String description;

	@ElementCollection
	@CollectionTable(name = "pokemon_type", joinColumns = @JoinColumn(name = "pokemon_id"))
	@OrderColumn(name = "sort_order")
	@Column(name = "type", nullable = false, length = 30)
	private List<String> types = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "pokemon_ability", joinColumns = @JoinColumn(name = "pokemon_id"))
	@OrderColumn(name = "sort_order")
	@Column(name = "ability", nullable = false, length = 100)
	private List<String> abilities = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "pokemon_stat", joinColumns = @JoinColumn(name = "pokemon_id"))
	@OrderColumn(name = "sort_order")
	private List<StatValue> stats = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "pokemon_evolution_stage", joinColumns = @JoinColumn(name = "pokemon_id"))
	@OrderColumn(name = "sort_order")
	private List<EvolutionStageValue> evolution = new ArrayList<>();

	// --- proprietary -----------------------------------------------------------------------------

	@Column(name = "local_name", length = 100)
	private String localName;

	@Column(name = "local_region", length = 100)
	private String localRegion;

	@ElementCollection
	@CollectionTable(name = "pokemon_tag", joinColumns = @JoinColumn(name = "pokemon_id"))
	@Column(name = "tag", nullable = false, length = 30)
	@OrderBy
	private Set<String> tags = new LinkedHashSet<>();

	// --- control ---------------------------------------------------------------------------------

	@Version
	@Column(nullable = false)
	private long version;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected PokemonEntity() {
		// for JPA
	}

	static PokemonEntity newImport(LocalPokemon pokemon) {
		PokemonDetail data = pokemon.pokeApiData();
		PokemonEntity entity = new PokemonEntity();
		entity.pokeapiId = data.id();
		entity.name = data.name();
		entity.spriteUrl = data.spriteUrl();
		entity.imageUrl = data.imageUrl();
		entity.category = data.category();
		entity.heightM = data.heightM();
		entity.weightKg = data.weightKg();
		entity.description = data.description();
		entity.types.addAll(data.types());
		entity.abilities.addAll(data.abilities());
		data.stats().forEach(stat -> entity.stats.add(new StatValue(stat.name(), stat.baseValue())));
		data.evolution().forEach(stage -> entity.evolution.add(new EvolutionStageValue(stage)));
		entity.localName = pokemon.localName();
		entity.localRegion = pokemon.localRegion();
		entity.tags.addAll(pokemon.tags());
		return entity;
	}

	@PrePersist
	void onCreate() {
		// PostgreSQL keeps microseconds: truncate, so the saved value equals the one read back later.
		createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
		updatedAt = createdAt;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
	}

	/** Must run inside a transaction: the lists are loaded lazily. */
	LocalPokemon toDomain() {
		PokemonDetail data = new PokemonDetail(pokeapiId, name, spriteUrl, imageUrl, category, types, heightM,
				weightKg, abilities, stats.stream().map(StatValue::toDomain).toList(), description,
				evolution.stream().map(EvolutionStageValue::toDomain).toList());
		return new LocalPokemon(id, data, localName, localRegion, List.copyOf(tags), version, createdAt, updatedAt);
	}

	@Embeddable
	static class StatValue {

		@Column(name = "name", nullable = false, length = 50)
		private String name;

		@Column(name = "base_value", nullable = false)
		private int baseValue;

		protected StatValue() {
			// for JPA
		}

		StatValue(String name, int baseValue) {
			this.name = name;
			this.baseValue = baseValue;
		}

		Stat toDomain() {
			return new Stat(name, baseValue);
		}
	}

	@Embeddable
	static class EvolutionStageValue {

		@Column(name = "stage", nullable = false)
		private int stage;

		@Column(name = "species_id", nullable = false)
		private int speciesId;

		@Column(name = "name", nullable = false, length = 100)
		private String name;

		@Column(name = "evolves_from", length = 100)
		private String evolvesFrom;

		protected EvolutionStageValue() {
			// for JPA
		}

		EvolutionStageValue(EvolutionStage stage) {
			this.stage = stage.stage();
			this.speciesId = stage.speciesId();
			this.name = stage.name();
			this.evolvesFrom = stage.evolvesFrom();
		}

		EvolutionStage toDomain() {
			return new EvolutionStage(stage, speciesId, name, evolvesFrom);
		}
	}
}
