package com.example.pokedex.web;

import com.example.pokedex.domain.shared.Page;
import java.util.List;
import java.util.function.Function;

/**
 * The common shape of every paginated response.
 *
 * @param page zero-based page number
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

	public static <D, T> PageResponse<T> from(Page<D> page, Function<D, T> mapper) {
		return new PageResponse<>(page.items().stream().map(mapper).toList(), page.page(), page.size(),
				page.totalItems(), page.totalPages());
	}
}
