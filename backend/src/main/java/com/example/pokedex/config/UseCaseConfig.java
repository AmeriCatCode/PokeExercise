package com.example.pokedex.config;

import com.example.pokedex.application.port.LocalPokemonRepositoryPort;
import com.example.pokedex.application.port.PasswordHasherPort;
import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.application.port.TokenIssuerPort;
import com.example.pokedex.application.port.UserRepositoryPort;
import com.example.pokedex.application.usecase.DeleteLocalPokemonUseCase;
import com.example.pokedex.application.usecase.GetImportStatusUseCase;
import com.example.pokedex.application.usecase.GetLocalPokemonUseCase;
import com.example.pokedex.application.usecase.GetPokemonDetailUseCase;
import com.example.pokedex.application.usecase.ImportPokemonUseCase;
import com.example.pokedex.application.usecase.ListLocalPokemonUseCase;
import com.example.pokedex.application.usecase.ListPokemonUseCase;
import com.example.pokedex.application.usecase.LoginUseCase;
import com.example.pokedex.application.usecase.RegisterUserUseCase;
import com.example.pokedex.application.usecase.UpdateLocalPokemonUseCase;
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
	ImportPokemonUseCase importPokemonUseCase(PokemonCatalogPort catalog, LocalPokemonRepositoryPort repository) {
		return new ImportPokemonUseCase(catalog, repository);
	}

	@Bean
	ListLocalPokemonUseCase listLocalPokemonUseCase(LocalPokemonRepositoryPort repository) {
		return new ListLocalPokemonUseCase(repository);
	}

	@Bean
	GetLocalPokemonUseCase getLocalPokemonUseCase(LocalPokemonRepositoryPort repository) {
		return new GetLocalPokemonUseCase(repository);
	}

	@Bean
	UpdateLocalPokemonUseCase updateLocalPokemonUseCase(LocalPokemonRepositoryPort repository) {
		return new UpdateLocalPokemonUseCase(repository);
	}

	@Bean
	DeleteLocalPokemonUseCase deleteLocalPokemonUseCase(LocalPokemonRepositoryPort repository) {
		return new DeleteLocalPokemonUseCase(repository);
	}

	@Bean
	GetImportStatusUseCase getImportStatusUseCase(LocalPokemonRepositoryPort repository) {
		return new GetImportStatusUseCase(repository);
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
