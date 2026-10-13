package com.example.pokedex.application.port;

import com.example.pokedex.domain.exception.UserAlreadyExistsException;
import com.example.pokedex.domain.user.User;
import java.util.Optional;

/**
 * Storage of registered users.
 */
public interface UserRepositoryPort {

	/** @param username already normalized (lower case) */
	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	/**
	 * @throws UserAlreadyExistsException if the username or email is taken, including when another
	 *                                    registration with the same data is saved at the same moment
	 */
	User save(User user);
}
