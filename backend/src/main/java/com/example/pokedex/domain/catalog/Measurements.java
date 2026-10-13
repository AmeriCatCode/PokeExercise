package com.example.pokedex.domain.catalog;

/**
 * Unit conversions for the measurements PokeAPI returns.
 */
public final class Measurements {

	private Measurements() {
	}

	/** PokeAPI gives weights in hectograms (1 hg = 0.1 kg). */
	public static double hectogramsToKilograms(int hectograms) {
		return hectograms / 10.0;
	}

	/** PokeAPI gives heights in decimetres (1 dm = 0.1 m). */
	public static double decimetresToMetres(int decimetres) {
		return decimetres / 10.0;
	}
}
