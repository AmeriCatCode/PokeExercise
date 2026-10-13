package com.example.pokedex.domain.user;

import com.example.pokedex.domain.exception.InvalidRequestException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * The data of a new account, validated and normalized. Always valid once created.
 * <p>
 * Username and email are trimmed and lower-cased, so "Ash" and "ash" are the same user.
 */
public record Registration(String username, String email, String password) {

	private static final Pattern USERNAME = Pattern.compile("[a-z0-9_.-]{3,30}");
	private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
	private static final int MAX_EMAIL_LENGTH = 254;
	private static final int MIN_PASSWORD_LENGTH = 8;
	/** BCrypt only uses the first 72 bytes of a password. */
	private static final int MAX_PASSWORD_LENGTH = 72;
	private static final Pattern HAS_LETTER = Pattern.compile(".*\\p{L}.*");
	private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");

	public Registration {
		username = normalize(username);
		email = normalize(email);
		if (username == null || !USERNAME.matcher(username).matches()) {
			throw new InvalidRequestException("username",
					"must be 3 to 30 characters: letters, digits, '.', '_' or '-'");
		}
		if (email == null || email.length() > MAX_EMAIL_LENGTH || !EMAIL.matcher(email).matches()) {
			throw new InvalidRequestException("email", "must be a valid email address");
		}
		if (password == null || password.length() < MIN_PASSWORD_LENGTH
				|| password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_LENGTH
				|| !HAS_LETTER.matcher(password).matches() || !HAS_DIGIT.matcher(password).matches()) {
			throw new InvalidRequestException("password",
					"must be 8 to 72 characters and contain at least one letter and one digit");
		}
	}

	/** Usernames are stored and compared in this form. */
	public static String normalize(String value) {
		return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
	}

	@Override
	public String toString() {
		return "Registration[username=" + username + ", email=" + email + "]";
	}
}
