package com.example.pokedex.infrastructure.security;

import com.example.pokedex.application.port.TokenIssuerPort;
import com.example.pokedex.domain.user.IssuedToken;
import com.example.pokedex.domain.user.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * Issues signed JWTs: subject = username, plus the user id and role. They expire after the configured time.
 */
public class JwtTokenIssuer implements TokenIssuerPort {

	public static final String ISSUER = "pokedex";

	private final JwtEncoder encoder;
	private final Duration timeToLive;
	private final Clock clock;

	public JwtTokenIssuer(JwtEncoder encoder, Duration timeToLive, Clock clock) {
		this.encoder = encoder;
		this.timeToLive = timeToLive;
		this.clock = clock;
	}

	@Override
	public IssuedToken issue(User user) {
		// JWT times have a precision of one second.
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant expiresAt = now.plus(timeToLive);
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(ISSUER)
				.subject(user.username())
				.issuedAt(now)
				.expiresAt(expiresAt)
				.claim("uid", user.id())
				.claim("role", user.role().name())
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new IssuedToken(token, expiresAt);
	}
}
