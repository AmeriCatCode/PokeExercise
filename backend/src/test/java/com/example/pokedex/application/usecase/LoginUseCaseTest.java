package com.example.pokedex.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.application.port.TokenIssuerPort;
import com.example.pokedex.domain.exception.InvalidCredentialsException;
import com.example.pokedex.domain.user.IssuedToken;
import com.example.pokedex.domain.user.Role;
import com.example.pokedex.domain.user.User;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginUseCaseTest {

	private static final Instant EXPIRES_AT = Instant.parse("2026-10-12T11:00:00Z");
	private static final TokenIssuerPort FAKE_TOKENS = user -> new IssuedToken("token-for-" + user.username(), EXPIRES_AT);

	private final FakeUserRepository users = new FakeUserRepository();
	private final LoginUseCase useCase = new LoginUseCase(users, RegisterUserUseCaseTest.FAKE_HASHER, FAKE_TOKENS);

	@BeforeEach
	void registerAsh() {
		users.save(new User(null, "ash", "ash@example.com", "hashed:Pikachu25", Role.USER));
	}

	@Test
	void aSuccessfulLoginReturnsATokenAndTheUser() {
		LoginUseCase.LoginResult result = useCase.login("ash", "Pikachu25");

		assertThat(result.token()).isEqualTo(new IssuedToken("token-for-ash", EXPIRES_AT));
		assertThat(result.user().username()).isEqualTo("ash");
	}

	@Test
	void theUsernameIsNotCaseSensitive() {
		assertThat(useCase.login("  ASH ", "Pikachu25").user().username()).isEqualTo("ash");
	}

	@Test
	void aWrongPasswordIsRejected() {
		assertThatThrownBy(() -> useCase.login("ash", "Pikachu26")).isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void anUnknownUserGetsTheSameErrorAsAWrongPassword() {
		// Same error, so the answer doesn't reveal which usernames exist.
		assertThatThrownBy(() -> useCase.login("gary", "Pikachu25")).isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void missingCredentialsAreRejected() {
		assertThatThrownBy(() -> useCase.login(null, null)).isInstanceOf(InvalidCredentialsException.class);
	}
}
