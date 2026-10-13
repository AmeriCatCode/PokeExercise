package com.example.pokedex.domain.exception;

/**
 * Someone else changed the local Pokémon after the user loaded it: the version the user sent is no
 * longer the current one, so the update or delete is rejected instead of overwriting that change.
 */
public class VersionConflictException extends RuntimeException {

	public VersionConflictException(long id) {
		super("Local Pokémon " + id + " was changed by someone else");
	}
}
