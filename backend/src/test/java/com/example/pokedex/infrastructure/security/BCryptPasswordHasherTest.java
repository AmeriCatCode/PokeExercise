package com.example.pokedex.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

	private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

	@Test
	void theHashIsNotThePassword() {
		String hash = hasher.hash("Pikachu25");

		assertThat(hash).isNotEqualTo("Pikachu25").doesNotContain("Pikachu25").startsWith("$2");
	}

	@Test
	void theSamePasswordGetsADifferentHashEachTime() {
		assertThat(hasher.hash("Pikachu25")).isNotEqualTo(hasher.hash("Pikachu25"));
	}

	@Test
	void recognizesTheRightPasswordOnly() {
		String hash = hasher.hash("Pikachu25");

		assertThat(hasher.matches("Pikachu25", hash)).isTrue();
		assertThat(hasher.matches("Pikachu26", hash)).isFalse();
	}
}
