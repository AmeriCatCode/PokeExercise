package com.example.pokedex.infrastructure.security;

import java.time.Clock;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

	private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

	@Bean
	SecretKey jwtSecretKey(JwtProperties properties) {
		if (properties.secret() == null || properties.secret().isBlank()) {
			log.warn("No JWT_SECRET configured: using a random key. Login tokens stop working when the app "
					+ "restarts. Set JWT_SECRET (base64, at least 32 bytes) outside local development.");
			return JwtKeys.generate();
		}
		return JwtKeys.fromBase64(properties.secret());
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		return JwtKeys.encoder(jwtSecretKey);
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
		return JwtKeys.decoder(jwtSecretKey);
	}

	@Bean
	JwtTokenIssuer jwtTokenIssuer(JwtEncoder jwtEncoder, JwtProperties properties) {
		return new JwtTokenIssuer(jwtEncoder, properties.ttl(), Clock.systemUTC());
	}

	@Bean
	BCryptPasswordHasher passwordHasher() {
		return new BCryptPasswordHasher();
	}
}
