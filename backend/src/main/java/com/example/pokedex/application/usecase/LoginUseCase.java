package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.PasswordHasherPort;
import com.example.pokedex.application.port.TokenIssuerPort;
import com.example.pokedex.application.port.UserRepositoryPort;
import com.example.pokedex.domain.exception.InvalidCredentialsException;
import com.example.pokedex.domain.user.IssuedToken;
import com.example.pokedex.domain.user.Registration;
import com.example.pokedex.domain.user.User;
import java.util.Optional;

/**
 * Checks the credentials and issues a login token.
 */
public class LoginUseCase {

	public record LoginResult(IssuedToken token, User user) {
	}

	private final UserRepositoryPort users;
	private final PasswordHasherPort hasher;
	private final TokenIssuerPort tokens;

	public LoginUseCase(UserRepositoryPort users, PasswordHasherPort hasher, TokenIssuerPort tokens) {
		this.users = users;
		this.hasher = hasher;
		this.tokens = tokens;
	}

	/**
	 * @throws InvalidCredentialsException for an unknown user or a wrong password (same error for both)
	 */
	public LoginResult login(String username, String password) {
		if (username == null || password == null) {
			throw new InvalidCredentialsException();
		}
		Optional<User> user = users.findByUsername(Registration.normalize(username));
		if (user.isEmpty()) {
			// Spend the same time as a real password check, so response times don't reveal
			// which usernames exist.
			hasher.hash(password);
			throw new InvalidCredentialsException();
		}
		if (!hasher.matches(password, user.get().passwordHash())) {
			throw new InvalidCredentialsException();
		}
		return new LoginResult(tokens.issue(user.get()), user.get());
	}
}
