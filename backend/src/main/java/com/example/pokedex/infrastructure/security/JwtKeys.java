package com.example.pokedex.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * The HMAC-SHA256 key that signs and verifies login tokens, and the encoder/decoder built on it.
 */
public final class JwtKeys {

	private static final String ALGORITHM = "HmacSHA256";
	/** HS256 needs a key of at least 256 bits. */
	private static final int MIN_KEY_BYTES = 32;

	private JwtKeys() {
	}

	public static SecretKey fromBase64(String secret) {
		byte[] bytes = Base64.getDecoder().decode(secret);
		if (bytes.length < MIN_KEY_BYTES) {
			throw new IllegalArgumentException("The JWT secret must be at least 256 bits (32 bytes, base64-encoded)");
		}
		return new SecretKeySpec(bytes, ALGORITHM);
	}

	/** A random key: fine for development and tests, but tokens stop working when the app restarts. */
	public static SecretKey generate() {
		byte[] bytes = new byte[MIN_KEY_BYTES];
		new SecureRandom().nextBytes(bytes);
		return new SecretKeySpec(bytes, ALGORITHM);
	}

	public static JwtEncoder encoder(SecretKey key) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(key));
	}

	/** Checks the signature, the expiry and that the token was issued by this application. */
	public static JwtDecoder decoder(SecretKey key) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtTokenIssuer.ISSUER));
		return decoder;
	}
}
