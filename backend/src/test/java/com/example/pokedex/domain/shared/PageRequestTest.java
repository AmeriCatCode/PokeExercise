package com.example.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.domain.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PageRequestTest {

	@Test
	void calculatesTheOffsetOfThePage() {
		assertThat(new PageRequest(0, 20).offset()).isZero();
		assertThat(new PageRequest(3, 20).offset()).isEqualTo(60);
	}

	@Test
	void rejectsANegativePage() {
		assertThatThrownBy(() -> new PageRequest(-1, 20))
				.isInstanceOf(InvalidRequestException.class)
				.extracting("field").isEqualTo("page");
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -5, 51, 1000})
	void rejectsASizeOutsideOneToFifty(int size) {
		assertThatThrownBy(() -> new PageRequest(0, size))
				.isInstanceOf(InvalidRequestException.class)
				.extracting("field").isEqualTo("size");
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 20, 50})
	void acceptsASizeFromOneToFifty(int size) {
		assertThat(new PageRequest(0, size).size()).isEqualTo(size);
	}
}
