package com.example.pokedex.domain.catalog;

/**
 * One species of an evolution chain.
 *
 * @param stage        1 for the base species, 2 for its evolutions, and so on
 * @param speciesId    the species number in PokeAPI
 * @param evolvesFrom  the species it evolves from, or {@code null} for the base species
 */
public record EvolutionStage(int stage, int speciesId, String name, String evolvesFrom) {
}
