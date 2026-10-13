package com.example.pokedex.infrastructure.pokeapi;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * PokeAPI connection and cache settings ({@code pokeapi.*} in application.yml).
 */
@ConfigurationProperties("pokeapi")
public record PokeApiProperties(
		@DefaultValue("https://pokeapi.co/api/v2") String baseUrl,
		@DefaultValue("2s") Duration connectTimeout,
		@DefaultValue("5s") Duration readTimeout,
		@DefaultValue CacheSettings cache) {

	/**
	 * @param maxSize maximum entries per cache (Pokémon details, species)
	 * @param ttl     how long an entry is kept
	 */
	public record CacheSettings(
			@DefaultValue("1000") long maxSize,
			@DefaultValue("24h") Duration ttl) {
	}
}
