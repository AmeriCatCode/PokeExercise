package com.example.pokedex.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.domain.exception.InvalidRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IdOrNameTest {

	@ParameterizedTest
	@ValueSource(strings = {"25", "pikachu", "Mr-Mime"})
	void acceptsPokeApiNumbersAndNames(String value) {
		assertThat(IdOrName.requireValid(value)).isEqualTo(value);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"pika chu", "pika_chu", "../evil", "%2e%2e"})
	void rejectsAnythingElse(String value) {
		assertThatThrownBy(() -> IdOrName.requireValid(value))
				.isInstanceOf(InvalidRequestException.class)
				.extracting("field").isEqualTo("idOrName");
	}
}
