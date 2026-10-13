package com.example.pokedex.infrastructure.security;

import com.example.pokedex.application.port.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt: a slow, salted hash made for passwords. Each hash has its own random salt.
 */
public class BCryptPasswordHasher implements PasswordHasherPort {

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	@Override
	public String hash(String rawPassword) {
		return encoder.encode(rawPassword);
	}

	@Override
	public boolean matches(String rawPassword, String hash) {
		return encoder.matches(rawPassword, hash);
	}
}
