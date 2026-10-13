package com.example.pokedex.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.domain.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationTest {

	private static final String VALID_PASSWORD = "Pikachu25";

	@Test
	void acceptsAValidRegistration() {
		Registration registration = new Registration("ash", "ash@example.com", VALID_PASSWORD);

		assertThat(registration.username()).isEqualTo("ash");
		assertThat(registration.email()).isEqualTo("ash@example.com");
		assertThat(registration.password()).isEqualTo(VALID_PASSWORD);
	}

	@Test
	void normalizesUsernameAndEmailSoTheyAreUniqueWhateverTheCase() {
		Registration registration = new Registration("  Ash_Ketchum ", " Ash@Example.COM ", VALID_PASSWORD);

		assertThat(registration.username()).isEqualTo("ash_ketchum");
		assertThat(registration.email()).isEqualTo("ash@example.com");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"ab", "a-very-long-username-over-30-chars", "ash ketchum", "ash!", "élise"})
	void rejectsAnInvalidUsername(String username) {
		assertThatThrownBy(() -> new Registration(username, "ash@example.com", VALID_PASSWORD))
				.isInstanceOf(InvalidRequestException.class)
				.extracting("field").isEqualTo("username");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"ash", "ash@", "@example.com", "ash@example", "ash @example.com"})
	void rejectsAnInvalidEmail(String email) {
		assertThatThrownBy(() -> new Registration("ash", email, VALID_PASSWORD))
				.isInstanceOf(InvalidRequestException.class)
				.extracting("field").isEqualTo("email");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"Pika25", "onlyletters", "12345678"})
	void rejectsAWeakPassword(String password) {
		assertThatThrownBy(() -> new Registration("ash", "ash@example.com", password))
				.isInstanceOf(InvalidRequestException.class)
				.extracting("field").isEqualTo("password");
	}

	@Test
	void rejectsAPasswordLongerThanBCryptSupports() {
		assertThatThrownBy(() -> new Registration("ash", "ash@example.com", "a1".repeat(37)))
				.isInstanceOf(InvalidRequestException.class)
				.extracting("field").isEqualTo("password");
	}

	@Test
	void neverShowsThePasswordInItsTextForm() {
		assertThat(new Registration("ash", "ash@example.com", VALID_PASSWORD).toString()).doesNotContain(VALID_PASSWORD);
	}
}
