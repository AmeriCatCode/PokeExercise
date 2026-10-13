package com.example.pokedex.domain.catalog;

/**
 * A base statistic, e.g. {@code hp = 45}.
 *
 * @param name PokeAPI stat name: hp, attack, defense, special-attack, special-defense, speed
 */
public record Stat(String name, int baseValue) {
}
