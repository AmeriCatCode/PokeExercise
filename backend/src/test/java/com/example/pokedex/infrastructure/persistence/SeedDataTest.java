package com.example.pokedex.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.pokedex.TestcontainersConfiguration;
import com.example.pokedex.domain.local.LocalPokemon;
import com.example.pokedex.domain.local.ProprietaryFields;
import com.example.pokedex.domain.shared.PageRequest;
import com.example.pokedex.domain.user.User;
import com.example.pokedex.infrastructure.security.BCryptPasswordHasher;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The demo data of {@code db/seed}: the documented credentials work, the imported Pokémon are complete,
 * and running the script again never overwrites what users changed. (The other tests run without it.)
 */
@DataJpaTest(properties = {
		"spring.jpa.hibernate.ddl-auto=validate",
		"spring.flyway.locations=classpath:db/migration,classpath:db/seed"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaUserRepositoryAdapter.class, JpaLocalPokemonRepositoryAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SeedDataTest {

	@Autowired
	private JpaUserRepositoryAdapter users;

	@Autowired
	private JpaLocalPokemonRepositoryAdapter pokemon;

	@Autowired
	private DataSource dataSource;

	@Test
	void theDocumentedDemoCredentialsWork() {
		BCryptPasswordHasher hasher = new BCryptPasswordHasher();

		User ash = users.findByUsername("ash").orElseThrow();
		User misty = users.findByUsername("misty").orElseThrow();

		assertThat(hasher.matches("Pikachu123", ash.passwordHash())).isTrue();
		assertThat(hasher.matches("Togepi123", misty.passwordHash())).isTrue();
		assertThat(ash.email()).isEqualTo("ash@example.com");
	}

	@Test
	void somePokemonAreAlreadyImportedWithTheirPokeApiData() {
		List<LocalPokemon> imported = pokemon.findAll(new PageRequest(0, 50)).items();

		assertThat(imported).extracting(LocalPokemon::pokeapiId).containsExactly(1, 4, 7, 25, 133);
		LocalPokemon bulbasaur = imported.getFirst();
		assertThat(bulbasaur.pokeApiData().name()).isEqualTo("bulbasaur");
		assertThat(bulbasaur.pokeApiData().category()).isEqualTo("Seed Pokémon");
		assertThat(bulbasaur.pokeApiData().types()).containsExactly("grass", "poison");
		assertThat(bulbasaur.pokeApiData().stats()).hasSize(6);
		assertThat(bulbasaur.pokeApiData().evolution()).extracting("name")
				.containsExactly("bulbasaur", "ivysaur", "venusaur");
		assertThat(bulbasaur.pokeApiData().description()).isNotBlank();
		assertThat(bulbasaur.localName()).isEqualTo("Bulbizarre");
		assertThat(bulbasaur.tags()).containsExactly("grass-starter", "kanto-starter");
		assertThat(bulbasaur.version()).isZero();
		// Some are left without local data, to show an untouched import.
		LocalPokemon eevee = imported.getLast();
		assertThat(eevee.localName()).isNull();
		assertThat(eevee.tags()).isEmpty();
	}

	@Test
	void runningTheSeedAgainNeverOverwritesUserChanges() {
		long id = pokemon.findIdByPokeapiId(25).orElseThrow();
		LocalPokemon edited = pokemon.update(id, pokemon.findById(id).orElseThrow().version(),
				new ProprietaryFields("Sparky", "Kanto", List.of("mine")));

		new ResourceDatabasePopulator(new ClassPathResource("db/seed/R__demo_data.sql")).execute(dataSource);

		assertThat(pokemon.findById(id)).contains(edited);
		assertThat(pokemon.findAll(new PageRequest(0, 50)).totalItems()).isEqualTo(5);
	}
}
