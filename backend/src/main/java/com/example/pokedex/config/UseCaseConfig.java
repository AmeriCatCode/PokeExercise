package com.example.pokedex.config;

import com.example.pokedex.application.port.PasswordHasherPort;
import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.application.port.TokenIssuerPort;
import com.example.pokedex.application.port.UserRepositoryPort;
import com.example.pokedex.application.usecase.GetPokemonDetailUseCase;
import com.example.pokedex.application.usecase.ListPokemonUseCase;
import com.example.pokedex.application.usecase.LoginUseCase;
import com.example.pokedex.application.usecase.RegisterUserUseCase;
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

	@Bean
	GetPokemonDetailUseCase getPokemonDetailUseCase(PokemonCatalogPort catalog) {
		return new GetPokemonDetailUseCase(catalog);
	}

	@Bean
	RegisterUserUseCase registerUserUseCase(UserRepositoryPort users, PasswordHasherPort hasher) {
		return new RegisterUserUseCase(users, hasher);
	}

	@Bean
	LoginUseCase loginUseCase(UserRepositoryPort users, PasswordHasherPort hasher, TokenIssuerPort tokens) {
		return new LoginUseCase(users, hasher, tokens);
	}
}
