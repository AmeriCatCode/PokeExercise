package com.example.pokedex.domain.catalog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Turns an evolution tree into an ordered list of stages.
 */
public final class EvolutionChain {

	/** A species of the evolution tree and the species it can evolve into. */
	public record Node(int speciesId, String name, List<Node> evolvesTo) {

		public Node {
			evolvesTo = List.copyOf(evolvesTo);
		}
	}

	private EvolutionChain() {
	}

	/**
	 * Lists the species stage by stage (base species first). Within a stage, species keep the tree order.
	 * Each stage records the species it evolves from, so branches (e.g. Wurmple's) can be told apart.
	 */
	public static List<EvolutionStage> flatten(Node root) {
		record Pending(Node node, int stage, String evolvesFrom) {
		}

		List<EvolutionStage> stages = new ArrayList<>();
		Deque<Pending> queue = new ArrayDeque<>();
		queue.add(new Pending(root, 1, null));
		while (!queue.isEmpty()) {
			Pending current = queue.removeFirst();
			Node node = current.node();
			stages.add(new EvolutionStage(current.stage(), node.speciesId(), node.name(), current.evolvesFrom()));
			for (Node next : node.evolvesTo()) {
				queue.addLast(new Pending(next, current.stage() + 1, node.name()));
			}
		}
		return stages;
	}
}
