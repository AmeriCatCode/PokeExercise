package com.example.pokedex.infrastructure.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Login token settings ({@code security.jwt.*}).
 *
 * @param secret base64-encoded HMAC key of at least 256 bits; when empty, a random key is generated at startup
 * @param ttl    how long a login token is valid
 */
@ConfigurationProperties("security.jwt")
public record JwtProperties(String secret, @DefaultValue("1h") Duration ttl) {
}
