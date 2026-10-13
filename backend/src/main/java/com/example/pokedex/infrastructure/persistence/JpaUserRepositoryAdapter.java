package com.example.pokedex.infrastructure.persistence;

import com.example.pokedex.application.port.UserRepositoryPort;
import com.example.pokedex.domain.exception.UserAlreadyExistsException;
import com.example.pokedex.domain.user.User;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * {@link UserRepositoryPort} on PostgreSQL. The unique constraints of the {@code app_user} table are the
 * final judge of duplicates, so two registrations arriving at the same moment can never both succeed;
 * the losing one is reported as {@link UserAlreadyExistsException}, never as a database error.
 */
@Component
class JpaUserRepositoryAdapter implements UserRepositoryPort {

	private static final String USERNAME_CONSTRAINT = "uk_app_user_username";
	private static final String EMAIL_CONSTRAINT = "uk_app_user_email";

	private final UserJpaRepository jpa;

	JpaUserRepositoryAdapter(UserJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Optional<User> findByUsername(String username) {
		return jpa.findByUsername(username).map(UserEntity::toDomain);
	}

	@Override
	public boolean existsByUsername(String username) {
		return jpa.existsByUsername(username);
	}

	@Override
	public boolean existsByEmail(String email) {
		return jpa.existsByEmail(email);
	}

	@Override
	public User save(User user) {
		try {
			return jpa.saveAndFlush(UserEntity.from(user)).toDomain();
		}
		catch (DataIntegrityViolationException ex) {
			throw translate(ex);
		}
	}

	private static RuntimeException translate(DataIntegrityViolationException ex) {
		String constraint = ex.getCause() instanceof ConstraintViolationException violation
				? violation.getConstraintName()
				: null;
		if (constraint != null && constraint.toLowerCase().contains(USERNAME_CONSTRAINT)) {
			return new UserAlreadyExistsException("username");
		}
		if (constraint != null && constraint.toLowerCase().contains(EMAIL_CONSTRAINT)) {
			return new UserAlreadyExistsException("email");
		}
		return ex;
	}
}
