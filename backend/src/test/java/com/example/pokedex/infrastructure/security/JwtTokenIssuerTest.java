package com.example.pokedex.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pokedex.domain.user.IssuedToken;
import com.example.pokedex.domain.user.Role;
import com.example.pokedex.domain.user.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtTokenIssuerTest {

	private static final User ASH = new User(7L, "ash", "ash@example.com", "hash", Role.USER);
	private static final Duration TTL = Duration.ofHours(1);

	private final SecretKey key = JwtKeys.generate();
	private final JwtDecoder decoder = JwtKeys.decoder(key);

	private JwtTokenIssuer issuerAt(Instant now) {
		return new JwtTokenIssuer(JwtKeys.encoder(key), TTL, Clock.fixed(now, ZoneOffset.UTC));
	}

	@Test
	void issuesATokenThatIdentifiesTheUser() {
		Instant now = Instant.now();

		IssuedToken token = issuerAt(now).issue(ASH);
		Jwt jwt = decoder.decode(token.value());

		assertThat(jwt.getSubject()).isEqualTo("ash");
		assertThat(jwt.getClaimAsString("uid")).isEqualTo("7");
		assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
		assertThat(jwt.getClaimAsString("iss")).isEqualTo(JwtTokenIssuer.ISSUER);
		assertThat(token.expiresAt()).isEqualTo(now.plus(TTL).truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
	}

	@Test
	void anExpiredTokenIsRejected() {
		IssuedToken token = issuerAt(Instant.now().minus(Duration.ofHours(2))).issue(ASH);

		assertThatThrownBy(() -> decoder.decode(token.value())).isInstanceOf(JwtException.class)
				.hasMessageContaining("expired");
	}

	@Test
	void aTamperedTokenIsRejected() {
		String token = issuerAt(Instant.now()).issue(ASH).value();
		String[] parts = token.split("\\.");
		// Change the payload (e.g. to claim another user) while keeping the original signature.
		String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
				.encodeToString("{\"sub\":\"admin\",\"role\":\"ADMIN\",\"exp\":9999999999}".getBytes());
		String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

		assertThatThrownBy(() -> decoder.decode(tampered)).isInstanceOf(JwtException.class);
	}

	@Test
	void aTokenSignedWithAnotherKeyIsRejected() {
		String token = new JwtTokenIssuer(JwtKeys.encoder(JwtKeys.generate()), TTL, Clock.systemUTC()).issue(ASH).value();

		assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
	}

	@Test
	void theKeyCanBeReadFromABase64Secret() {
		String secret = java.util.Base64.getEncoder().encodeToString(new byte[32]);

		assertThat(JwtKeys.fromBase64(secret).getEncoded()).hasSize(32);
	}

	@Test
	void aSecretShorterThan256BitsIsRefused() {
		String shortSecret = java.util.Base64.getEncoder().encodeToString(new byte[16]);

		assertThatThrownBy(() -> JwtKeys.fromBase64(shortSecret)).isInstanceOf(IllegalArgumentException.class);
	}
}
