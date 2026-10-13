package com.example.pokedex.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MeasurementsTest {

	@Test
	void convertsHectogramsToKilograms() {
		assertThat(Measurements.hectogramsToKilograms(69)).isEqualTo(6.9);
		assertThat(Measurements.hectogramsToKilograms(9050)).isEqualTo(905.0);
		assertThat(Measurements.hectogramsToKilograms(0)).isEqualTo(0.0);
	}
}
