package com.example.pokedex.application.port;

/**
 * One-way password hashing.
 */
public interface PasswordHasherPort {

	String hash(String rawPassword);

	boolean matches(String rawPassword, String hash);
}
