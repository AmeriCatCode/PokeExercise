package com.example.pokedex.application.usecase;

import com.example.pokedex.application.port.UserRepositoryPort;
import com.example.pokedex.domain.exception.UserAlreadyExistsException;
import com.example.pokedex.domain.user.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory {@link UserRepositoryPort} with the same unique rules as the database. */
class FakeUserRepository implements UserRepositoryPort {

	private final List<User> users = new ArrayList<>();
	private final AtomicLong ids = new AtomicLong();

	@Override
	public Optional<User> findByUsername(String username) {
		return users.stream().filter(u -> u.username().equals(username)).findFirst();
	}

	@Override
	public boolean existsByUsername(String username) {
		return findByUsername(username).isPresent();
	}

	@Override
	public boolean existsByEmail(String email) {
		return users.stream().anyMatch(u -> u.email().equals(email));
	}

	@Override
	public User save(User user) {
		if (existsByUsername(user.username())) {
			throw new UserAlreadyExistsException("username");
		}
		if (existsByEmail(user.email())) {
			throw new UserAlreadyExistsException("email");
		}
		User saved = new User(ids.incrementAndGet(), user.username(), user.email(), user.passwordHash(), user.role());
		users.add(saved);
		return saved;
	}

	List<User> all() {
		return users;
	}
}
