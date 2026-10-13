package com.example.pokedex.infrastructure.pokeapi;

import com.github.benmanes.caffeine.cache.Ticker;
import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PokeApiProperties.class)
public class PokeApiConfig {

	/** A RestClient for PokeAPI with connect and read timeouts, so a slow PokeAPI never hangs a request. */
	public static RestClient restClient(PokeApiProperties properties) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.connectTimeout())
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.readTimeout());
		return RestClient.builder()
				.baseUrl(properties.baseUrl())
				.requestFactory(requestFactory)
				.build();
	}

	@Bean
	PokeApiClient pokeApiClient(PokeApiProperties properties) {
		return new PokeApiClient(restClient(properties));
	}

	@Bean
	PokeApiCache pokeApiCache(PokeApiClient client, PokeApiProperties properties) {
		return new PokeApiCache(client, properties.cache(), Ticker.systemTicker());
	}

	@Bean
	PokeApiCatalogAdapter pokeApiCatalogAdapter(PokeApiClient client, PokeApiCache cache) {
		return new PokeApiCatalogAdapter(client, cache);
	}
}
