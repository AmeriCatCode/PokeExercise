package com.example.pokedex.config;

import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.application.usecase.ListPokemonUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the use cases as Spring beans, so the application layer itself stays free of Spring.
 */
@Configuration
public class UseCaseConfig {

	@Bean
	ListPokemonUseCase listPokemonUseCase(PokemonCatalogPort catalog) {
		return new ListPokemonUseCase(catalog);
	}
}
