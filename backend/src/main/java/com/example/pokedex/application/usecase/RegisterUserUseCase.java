package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.PasswordHasherPort;
import com.example.pokedex.application.port.UserRepositoryPort;
import com.example.pokedex.domain.exception.UserAlreadyExistsException;
import com.example.pokedex.domain.user.Registration;
import com.example.pokedex.domain.user.Role;
import com.example.pokedex.domain.user.User;

/**
 * Creates a new account.
 */
public class RegisterUserUseCase {

	private final UserRepositoryPort users;
	private final PasswordHasherPort hasher;

	public RegisterUserUseCase(UserRepositoryPort users, PasswordHasherPort hasher) {
		this.users = users;
		this.hasher = hasher;
	}

	/**
	 * @throws com.example.pokedex.domain.exception.InvalidRequestException if the data is invalid
	 * @throws UserAlreadyExistsException                                   if the username or email is taken
	 */
	public User register(String username, String email, String password) {
		Registration registration = new Registration(username, email, password);
		// Friendly early check; the unique constraints of the database still decide when two
		// registrations with the same data arrive at the same moment.
		if (users.existsByUsername(registration.username())) {
			throw new UserAlreadyExistsException("username");
		}
		if (users.existsByEmail(registration.email())) {
			throw new UserAlreadyExistsException("email");
		}
		return users.save(new User(null, registration.username(), registration.email(),
				hasher.hash(registration.password()), Role.USER));
	}
}
