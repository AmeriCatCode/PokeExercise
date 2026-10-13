package com.example.pokedex.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.TestcontainersConfiguration;
import com.example.pokedex.domain.exception.UserAlreadyExistsException;
import com.example.pokedex.domain.user.Role;
import com.example.pokedex.domain.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs against a real PostgreSQL, because the unique rules that matter here live in the database.
 * No test-wide transaction: each save commits on its own, as in production (a rejected insert would
 * otherwise break the shared transaction).
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaUserRepositoryAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JpaUserRepositoryAdapterTest {

	@Autowired
	private JpaUserRepositoryAdapter users;

	@Autowired
	private UserJpaRepository jpa;

	@AfterEach
	void cleanUp() {
		jpa.deleteAll();
	}

	@Test
	void savesAndFindsAUser() {
		User saved = users.save(new User(null, "ash", "ash@example.com", "$2a$10$hash", Role.USER));

		assertThat(saved.id()).isNotNull();
		assertThat(users.findByUsername("ash")).contains(saved);
		assertThat(users.existsByUsername("ash")).isTrue();
		assertThat(users.existsByEmail("ash@example.com")).isTrue();
		assertThat(users.findByUsername("misty")).isEmpty();
	}

	@Test
	void theDatabaseRejectsADuplicateUsername() {
		users.save(new User(null, "ash", "ash@example.com", "$2a$10$hash", Role.USER));

		assertThatThrownBy(() -> users.save(new User(null, "ash", "other@example.com", "$2a$10$hash", Role.USER)))
				.isInstanceOf(UserAlreadyExistsException.class)
				.extracting("field").isEqualTo("username");
	}

	@Test
	void theDatabaseRejectsADuplicateEmail() {
		users.save(new User(null, "ash", "ash@example.com", "$2a$10$hash", Role.USER));

		assertThatThrownBy(() -> users.save(new User(null, "misty", "ash@example.com", "$2a$10$hash", Role.USER)))
				.isInstanceOf(UserAlreadyExistsException.class)
				.extracting("field").isEqualTo("email");
	}

	@Test
	void simultaneousRegistrationsWithTheSameUsernameSaveExactlyOne() throws Exception {
		int attempts = 5;
		CountDownLatch start = new CountDownLatch(1);
		List<Future<User>> results = new ArrayList<>();
		try (ExecutorService executor = Executors.newFixedThreadPool(attempts)) {
			for (int i = 0; i < attempts; i++) {
				String email = "ash" + i + "@example.com";
				Callable<User> register = () -> {
					start.await();
					return users.save(new User(null, "ash", email, "$2a$10$hash", Role.USER));
				};
				results.add(executor.submit(register));
			}
			start.countDown();

			int saved = 0;
			int rejected = 0;
			for (Future<User> result : results) {
				try {
					result.get();
					saved++;
				}
				catch (ExecutionException ex) {
					assertThat(ex.getCause()).isInstanceOf(UserAlreadyExistsException.class);
					rejected++;
				}
			}
			assertThat(saved).isEqualTo(1);
			assertThat(rejected).isEqualTo(attempts - 1);
		}
		assertThat(jpa.count()).isEqualTo(1);
	}
}
