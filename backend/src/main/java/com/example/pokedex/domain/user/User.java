package com.example.pokedex.domain.user;

/**
 * A registered user.
 *
 * @param id           {@code null} until the user is saved
 * @param username     lower case, unique
 * @param email        lower case, unique
 * @param passwordHash one-way hash of the password; the password itself is never stored
 */
public record User(Long id, String username, String email, String passwordHash, Role role) {

	@Override
	public String toString() {
		return "User[id=" + id + ", username=" + username + ", role=" + role + "]";
	}
}
