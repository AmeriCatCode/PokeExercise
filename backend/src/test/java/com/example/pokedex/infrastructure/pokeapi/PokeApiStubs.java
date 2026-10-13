package com.example.pokedex.infrastructure.pokeapi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds a fake PokeAPI in WireMock with the minimal JSON our client reads.
 */
final class PokeApiStubs {

	private final WireMockExtension wm;

	PokeApiStubs(WireMockExtension wm) {
		this.wm = wm;
	}

	static PokeApiProperties properties(WireMockExtension wm, Duration readTimeout) {
		return new PokeApiProperties(wm.baseUrl(), Duration.ofSeconds(1), readTimeout,
				new PokeApiProperties.CacheSettings(1000, Duration.ofHours(24)));
	}

	/** The list page answered for {@code offset}/{@code limit}, containing the given Pokémon ids. */
	void list(int offset, int limit, int count, List<Integer> ids) {
		String results = ids.stream()
				.map(id -> """
						{"name": "%s", "url": "https://pokeapi.co/api/v2/pokemon/%d/"}""".formatted(nameOf(id), id))
				.collect(Collectors.joining(","));
		wm.stubFor(get(urlPathEqualTo("/pokemon"))
				.withQueryParam("offset", equalTo(String.valueOf(offset)))
				.withQueryParam("limit", equalTo(String.valueOf(limit)))
				.willReturn(okJson("""
						{"count": %d, "next": null, "previous": null, "results": [%s]}""".formatted(count, results))));
	}

	/** A Pokémon whose species has the same id. */
	void pokemon(int id) {
		pokemon(id, id, Duration.ZERO);
	}

	void pokemon(int id, int speciesId, Duration delay) {
		wm.stubFor(get(urlPathEqualTo("/pokemon/" + id))
				.willReturn(okJson(pokemonJson(id, speciesId)).withFixedDelay((int) delay.toMillis())));
	}

	void pokemonByName(String name, int id) {
		wm.stubFor(get(urlPathEqualTo("/pokemon/" + name)).willReturn(okJson(pokemonJson(id, id))));
	}

	void species(int id, String englishGenus) {
		species(id, englishGenus, Duration.ZERO);
	}

	void species(int id, String englishGenus, Duration delay) {
		wm.stubFor(get(urlPathEqualTo("/pokemon-species/" + id))
				.willReturn(okJson("""
						{"id": %d, "name": "%s",
						 "genera": [
						  {"genus": "たねポケモン", "language": {"name": "ja", "url": "https://pokeapi.co/api/v2/language/1/"}},
						  {"genus": "%s", "language": {"name": "en", "url": "https://pokeapi.co/api/v2/language/9/"}}
						 ],
						 "flavor_text_entries": [
						  {"flavor_text": "Old text.", "language": {"name": "en", "url": "https://pokeapi.co/api/v2/language/9/"},
						   "version": {"name": "red", "url": "https://pokeapi.co/api/v2/version/1/"}},
						  {"flavor_text": "A strange seed was\\nplanted on its\\nback at birth.", "language": {"name": "en", "url": "https://pokeapi.co/api/v2/language/9/"},
						   "version": {"name": "shield", "url": "https://pokeapi.co/api/v2/version/34/"}},
						  {"flavor_text": "Texte français.", "language": {"name": "fr", "url": "https://pokeapi.co/api/v2/language/5/"},
						   "version": {"name": "shield", "url": "https://pokeapi.co/api/v2/version/34/"}}
						 ],
						 "evolution_chain": {"url": "https://pokeapi.co/api/v2/evolution-chain/%d/"}
						}""".formatted(id, nameOf(id), englishGenus, CHAIN_ID)).withFixedDelay((int) delay.toMillis())));
	}

	/** Every species of these stubs belongs to the Bulbasaur → Ivysaur → Venusaur chain. */
	static final int CHAIN_ID = 1;

	void evolutionChain() {
		wm.stubFor(get(urlPathEqualTo("/evolution-chain/" + CHAIN_ID))
				.willReturn(okJson("""
						{"id": 1, "baby_trigger_item": null, "chain": {
						  "is_baby": false,
						  "species": {"name": "bulbasaur", "url": "https://pokeapi.co/api/v2/pokemon-species/1/"},
						  "evolution_details": [],
						  "evolves_to": [{
						    "species": {"name": "ivysaur", "url": "https://pokeapi.co/api/v2/pokemon-species/2/"},
						    "evolves_to": [{
						      "species": {"name": "venusaur", "url": "https://pokeapi.co/api/v2/pokemon-species/3/"},
						      "evolves_to": []
						    }]
						  }]
						}}""")));
	}

	/** Pokémon, species and evolution chain of Bulbasaur, everything the detail view needs. */
	void bulbasaurDetail() {
		pokemon(1);
		pokemonByName("bulbasaur", 1);
		species(1, "Seed Pokémon");
		evolutionChain();
	}

	void status(String path, int status) {
		wm.stubFor(get(urlPathEqualTo(path)).willReturn(aResponse().withStatus(status)));
	}

	static String nameOf(int id) {
		return switch (id) {
			case 1 -> "bulbasaur";
			case 2 -> "ivysaur";
			case 3 -> "venusaur";
			case 25 -> "pikachu";
			case 10033 -> "venusaur-mega";
			default -> "pokemon-" + id;
		};
	}

	private static String pokemonJson(int id, int speciesId) {
		// Types and abilities are deliberately out of slot order: the client must sort them.
		return """
				{
				  "id": %d,
				  "name": "%s",
				  "weight": 69,
				  "height": 7,
				  "sprites": {
				    "front_default": "https://img.example/%1$d.png",
				    "back_default": null,
				    "other": {"official-artwork": {"front_default": "https://img.example/artwork/%1$d.png"}}
				  },
				  "stats": [
				    {"base_stat": 45, "effort": 0, "stat": {"name": "hp", "url": "https://pokeapi.co/api/v2/stat/1/"}},
				    {"base_stat": 49, "effort": 0, "stat": {"name": "attack", "url": "https://pokeapi.co/api/v2/stat/2/"}},
				    {"base_stat": 49, "effort": 0, "stat": {"name": "defense", "url": "https://pokeapi.co/api/v2/stat/3/"}},
				    {"base_stat": 65, "effort": 1, "stat": {"name": "special-attack", "url": "https://pokeapi.co/api/v2/stat/4/"}},
				    {"base_stat": 65, "effort": 0, "stat": {"name": "special-defense", "url": "https://pokeapi.co/api/v2/stat/5/"}},
				    {"base_stat": 45, "effort": 0, "stat": {"name": "speed", "url": "https://pokeapi.co/api/v2/stat/6/"}}
				  ],
				  "types": [
				    {"slot": 2, "type": {"name": "poison", "url": "https://pokeapi.co/api/v2/type/4/"}},
				    {"slot": 1, "type": {"name": "grass", "url": "https://pokeapi.co/api/v2/type/12/"}}
				  ],
				  "abilities": [
				    {"slot": 3, "is_hidden": true, "ability": {"name": "chlorophyll", "url": "https://pokeapi.co/api/v2/ability/34/"}},
				    {"slot": 1, "is_hidden": false, "ability": {"name": "overgrow", "url": "https://pokeapi.co/api/v2/ability/65/"}}
				  ],
				  "species": {"name": "%s", "url": "https://pokeapi.co/api/v2/pokemon-species/%d/"}
				}""".formatted(id, nameOf(id), nameOf(speciesId), speciesId);
	}
}
