package com.example.pokedex.web.auth;

import com.example.pokedex.domain.user.User;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

/**
 * Request and response bodies of the authentication API. Detailed rules (lengths, formats) live in the
 * domain; here only the presence of each field is checked, so every missing field is reported at once.
 */
final class AuthDtos {

	private AuthDtos() {
	}

	record RegisterRequest(@NotBlank String username, @NotBlank String email, @NotBlank String password) {

		@Override
		public String toString() {
			return "RegisterRequest[username=" + username + ", email=" + email + "]";
		}
	}

	record LoginRequest(@NotBlank String username, @NotBlank String password) {

		@Override
		public String toString() {
			return "LoginRequest[username=" + username + "]";
		}
	}

	/** A user, as returned by the API: never the password hash. */
	record UserResponse(Long id, String username, String email, String role) {

		static UserResponse from(User user) {
			return new UserResponse(user.id(), user.username(), user.email(), user.role().name());
		}
	}

	/**
	 * @param tokenType always "Bearer": send the token as {@code Authorization: Bearer <token>}
	 */
	record LoginResponse(String token, String tokenType, Instant expiresAt, UserResponse user) {
	}

	/** The logged-in user, read from the login token. */
	record CurrentUserResponse(Long id, String username, String role) {
	}
}
