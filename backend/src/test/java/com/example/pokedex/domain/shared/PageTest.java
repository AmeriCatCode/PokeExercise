package com.example.pokedex.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageTest {

	@Test
	void calculatesTotalPagesRoundingUp() {
		assertThat(new Page<>(List.of(), 0, 20, 1302).totalPages()).isEqualTo(66);
	}

	@Test
	void calculatesTotalPagesForAnExactMultiple() {
		assertThat(new Page<>(List.of(), 0, 20, 40).totalPages()).isEqualTo(2);
	}

	@Test
	void hasNoPagesWhenThereAreNoItems() {
		assertThat(new Page<>(List.of(), 0, 20, 0).totalPages()).isZero();
	}

	@Test
	void keepsTheItemsUnmodifiable() {
		Page<String> page = new Page<>(new ArrayList<>(List.of("a")), 0, 20, 1);

		assertThatThrownBy(() -> page.items().add("b")).isInstanceOf(UnsupportedOperationException.class);
	}
}
