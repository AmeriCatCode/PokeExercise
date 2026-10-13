package com.example.pokedex.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.pokedex.domain.catalog.EvolutionChain.Node;
import java.util.List;
import org.junit.jupiter.api.Test;

class EvolutionChainTest {

	@Test
	void flattensALinearChainIntoOrderedStages() {
		Node chain = new Node(1, "bulbasaur", List.of(
				new Node(2, "ivysaur", List.of(
						new Node(3, "venusaur", List.of())))));

		assertThat(EvolutionChain.flatten(chain)).containsExactly(
				new EvolutionStage(1, 1, "bulbasaur", null),
				new EvolutionStage(2, 2, "ivysaur", "bulbasaur"),
				new EvolutionStage(3, 3, "venusaur", "ivysaur"));
	}

	@Test
	void handlesBranchingEvolutions() {
		Node chain = new Node(133, "eevee", List.of(
				new Node(134, "vaporeon", List.of()),
				new Node(135, "jolteon", List.of()),
				new Node(136, "flareon", List.of())));

		assertThat(EvolutionChain.flatten(chain)).containsExactly(
				new EvolutionStage(1, 133, "eevee", null),
				new EvolutionStage(2, 134, "vaporeon", "eevee"),
				new EvolutionStage(2, 135, "jolteon", "eevee"),
				new EvolutionStage(2, 136, "flareon", "eevee"));
	}

	@Test
	void keepsWhichSpeciesEachBranchEvolvesFrom() {
		// Wurmple → Silcoon → Beautifly and Wurmple → Cascoon → Dustox
		Node chain = new Node(265, "wurmple", List.of(
				new Node(266, "silcoon", List.of(new Node(267, "beautifly", List.of()))),
				new Node(268, "cascoon", List.of(new Node(269, "dustox", List.of())))));

		assertThat(EvolutionChain.flatten(chain)).containsExactly(
				new EvolutionStage(1, 265, "wurmple", null),
				new EvolutionStage(2, 266, "silcoon", "wurmple"),
				new EvolutionStage(2, 268, "cascoon", "wurmple"),
				new EvolutionStage(3, 267, "beautifly", "silcoon"),
				new EvolutionStage(3, 269, "dustox", "cascoon"));
	}

	@Test
	void aPokemonThatDoesNotEvolveHasASingleStage() {
		assertThat(EvolutionChain.flatten(new Node(128, "tauros", List.of())))
				.containsExactly(new EvolutionStage(1, 128, "tauros", null));
	}
}
