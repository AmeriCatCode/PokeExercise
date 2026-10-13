package com.example.pokedex.domain.shared;

import java.util.List;

/**
 * One page of results.
 *
 * @param page       zero-based page number
 * @param totalItems number of items across all pages
 */
public record Page<T>(List<T> items, int page, int size, long totalItems) {

	public Page {
		items = List.copyOf(items);
	}

	public int totalPages() {
		return (int) ((totalItems + size - 1) / size);
	}
}
