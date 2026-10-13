package com.example.pokedex.domain.shared;

import com.example.pokedex.domain.exception.InvalidRequestException;

/**
 * A request for one page of results. Always valid once created.
 *
 * @param page zero-based page number
 */
public record PageRequest(int page, int size) {

	public static final int DEFAULT_SIZE = 20;
	public static final int MAX_SIZE = 50;

	public PageRequest {
		if (page < 0) {
			throw new InvalidRequestException("page", "must be greater than or equal to 0");
		}
		if (size < 1 || size > MAX_SIZE) {
			throw new InvalidRequestException("size", "must be between 1 and " + MAX_SIZE);
		}
	}

	public int offset() {
		return page * size;
	}
}
