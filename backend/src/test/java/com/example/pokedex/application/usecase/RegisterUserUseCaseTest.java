package com.example.pokedex.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.application.port.PasswordHasherPort;
import com.example.pokedex.domain.exception.InvalidRequestException;
import com.example.pokedex.domain.exception.UserAlreadyExistsException;
import com.example.pokedex.domain.user.Role;
import com.example.pokedex.domain.user.User;
import org.junit.jupiter.api.Test;

class RegisterUserUseCaseTest {

	/** Reversible on purpose, so the test can tell a hash from the raw password. */
	static final PasswordHasherPort FAKE_HASHER = new PasswordHasherPort() {
		@Override
		public String hash(String rawPassword) {
			return "hashed:" + rawPassword;
		}

		@Override
		public boolean matches(String rawPassword, String hash) {
			return hash.equals("hashed:" + rawPassword);
		}
	};

	private final FakeUserRepository users = new FakeUserRepository();
	private final RegisterUserUseCase useCase = new RegisterUserUseCase(users, FAKE_HASHER);

	@Test
	void savesTheNewUserWithTheUserRole() {
		User user = useCase.register("Ash", "ash@example.com", "Pikachu25");

		assertThat(user.id()).isNotNull();
		assertThat(user.username()).isEqualTo("ash");
		assertThat(user.email()).isEqualTo("ash@example.com");
		assertThat(user.role()).isEqualTo(Role.USER);
		assertThat(users.all()).containsExactly(user);
	}

	@Test
	void storesTheHashedPasswordNeverThePlainOne() {
		User user = useCase.register("ash", "ash@example.com", "Pikachu25");

		assertThat(user.passwordHash()).isEqualTo("hashed:Pikachu25").isNotEqualTo("Pikachu25");
	}

	@Test
	void rejectsAUsernameThatAlreadyExistsWhateverTheCase() {
		useCase.register("ash", "ash@example.com", "Pikachu25");

		assertThatThrownBy(() -> useCase.register("ASH", "other@example.com", "Pikachu25"))
				.isInstanceOf(UserAlreadyExistsException.class)
				.extracting("field").isEqualTo("username");
	}

	@Test
	void rejectsAnEmailThatAlreadyExists() {
		useCase.register("ash", "ash@example.com", "Pikachu25");

		assertThatThrownBy(() -> useCase.register("misty", "ASH@example.com", "Togepi175"))
				.isInstanceOf(UserAlreadyExistsException.class)
				.extracting("field").isEqualTo("email");
	}

	@Test
	void rejectsInvalidDataWithoutSavingAnything() {
		assertThatThrownBy(() -> useCase.register("ash", "not-an-email", "Pikachu25"))
				.isInstanceOf(InvalidRequestException.class);

		assertThat(users.all()).isEmpty();
	}
}
