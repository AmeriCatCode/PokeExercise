package com.example.pokedex.domain.user;

import java.time.Instant;

/**
 * A login token and the moment it stops being valid.
 */
public record IssuedToken(String value, Instant expiresAt) {

	@Override
	public String toString() {
		return "IssuedToken[expiresAt=" + expiresAt + "]";
	}
}
