package com.example.pokedex.domain.exception;

/**
 * A value given by the user breaks a business rule.
 */
public class InvalidRequestException extends RuntimeException {

	private final String field;
	private final String reason;

	/**
	 * @param reason the rule that was broken, e.g. "must be between 1 and 50"
	 */
	public InvalidRequestException(String field, String reason) {
		super(field + " " + reason);
		this.field = field;
		this.reason = reason;
	}

	public String getField() {
		return field;
	}

	public String getReason() {
		return reason;
	}
}
