package com.example.pokedex.web.security;

import com.example.pokedex.web.error.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

/**
 * Which routes are public and which need a login token.
 * <p>
 * Stateless: no session and no cookie, every protected request carries its JWT in the
 * {@code Authorization: Bearer} header (which is also why CSRF protection is not needed).
 * Routes are closed by default; only the ones listed here are public.
 */
@Configuration
public class SecurityConfig {

	private final JsonMapper jsonMapper;

	public SecurityConfig(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.cors(cors -> {
				})
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(routes -> routes
						.requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/pokemon", "/api/pokemon/*").permitAll()
						.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
						.requestMatchers("/v3/api-docs", "/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
						.permitAll()
						.requestMatchers("/error").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(resourceServer -> resourceServer
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
						.authenticationEntryPoint(authenticationEntryPoint())
						.accessDeniedHandler(accessDeniedHandler()))
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint(authenticationEntryPoint())
						.accessDeniedHandler(accessDeniedHandler()));
		return http.build();
	}

	/** Allowed browser origins for cross-origin calls (the dev frontend uses a same-origin proxy instead). */
	@Bean
	CorsConfigurationSource corsConfigurationSource(
			@Value("${security.cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins) {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(allowedOrigins);
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/api/**", cors);
		return source;
	}

	/** The token's {@code role} claim becomes the authority {@code ROLE_USER} or {@code ROLE_ADMIN}. */
	private static JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("role");
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	/** 401 in the common error format: no token, or an expired / invalid one. */
	private AuthenticationEntryPoint authenticationEntryPoint() {
		return (request, response, ex) -> {
			String message = ex instanceof InvalidBearerTokenException
					? "Your session has expired or is invalid, please log in again"
					: "Authentication required";
			response.setHeader("WWW-Authenticate", "Bearer");
			write(response, HttpStatus.UNAUTHORIZED, message);
		};
	}

	/** 403 in the common error format: logged in, but not allowed. */
	private AccessDeniedHandler accessDeniedHandler() {
		return (request, response, ex) -> write(response, HttpStatus.FORBIDDEN, "You are not allowed to do this");
	}

	private void write(HttpServletResponse response, HttpStatus status, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.of(status.value(), message));
	}
}
