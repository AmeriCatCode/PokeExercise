package com.example.pokedex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.pokedex.application.port.PokemonCatalogPort;
import com.example.pokedex.domain.shared.Page;
import com.example.pokedex.domain.shared.PageRequest;
import com.example.pokedex.domain.user.Role;
import com.example.pokedex.domain.user.User;
import com.example.pokedex.infrastructure.security.JwtKeys;
import com.example.pokedex.infrastructure.security.JwtTokenIssuer;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Registration, login and public vs. protected routes, through the whole application:
 * HTTP → security → controllers → use cases → PostgreSQL (Testcontainers).
 */
@SpringBootTest(properties = "security.jwt.secret=" + AuthApiIntegrationTest.SECRET)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthApiIntegrationTest {

	static final String SECRET = "dGVzdC1zZWNyZXQtZm9yLXRoZS1wb2tlZGV4LWFwaS10ZXN0cw==";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbc;

	/** The catalog is not under test here; it only proves public routes need no token. */
	@MockitoBean
	private PokemonCatalogPort catalog;

	@AfterEach
	void deleteUsers() {
		jdbc.update("DELETE FROM app_user");
	}

	// --- registration ---------------------------------------------------------------------------

	@Test
	void registrationCreatesTheUserAndNeverReturnsThePassword() throws Exception {
		register("Ash", "Ash@Example.com", "Pikachu25")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.username").value("ash"))
				.andExpect(jsonPath("$.email").value("ash@example.com"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void theStoredPasswordIsHashed() throws Exception {
		register("ash", "ash@example.com", "Pikachu25");

		String stored = jdbc.queryForObject("SELECT password_hash FROM app_user WHERE username = 'ash'", String.class);
		assertThat(stored).isNotEqualTo("Pikachu25").startsWith("$2");
	}

	@Test
	void aDuplicateUsernameReturns409WhateverTheCase() throws Exception {
		register("ash", "ash@example.com", "Pikachu25");

		register("ASH", "other@example.com", "Pikachu25")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.message").value("This username is already taken"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("username"));
	}

	@Test
	void aDuplicateEmailReturns409() throws Exception {
		register("ash", "ash@example.com", "Pikachu25");

		register("misty", "ash@example.com", "Togepi175")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("This email is already registered"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
	}

	@Test
	void invalidDataReturns400WithTheField() throws Exception {
		register("ash", "not-an-email", "Pikachu25")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Invalid request"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("email"));
	}

	@Test
	void missingFieldsReturn400ListingEachOne() throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.length()").value(3))
				.andExpect(jsonPath("$.fieldErrors[*].field",
						containsInAnyOrder("username", "email", "password")));
	}

	@Test
	void aMalformedBodyReturns400() throws Exception {
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{\"username\": "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").value("Malformed request body"));
	}

	// --- login -----------------------------------------------------------------------------------

	@Test
	void aSuccessfulLoginReturnsABearerTokenAndTheUser() throws Exception {
		register("ash", "ash@example.com", "Pikachu25");

		login("Ash", "Pikachu25")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresAt").isString())
				.andExpect(jsonPath("$.user.username").value("ash"))
				.andExpect(jsonPath("$.user.passwordHash").doesNotExist());
	}

	@Test
	void wrongCredentialsReturn401() throws Exception {
		register("ash", "ash@example.com", "Pikachu25");

		login("ash", "WrongPassword1")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
		login("gary", "Pikachu25")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid username or password"));
	}

	// --- public vs. protected routes ---------------------------------------------------------------

	@Test
	void aProtectedRouteWithoutTokenReturns401InTheCommonFormat() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.message").value("Authentication required"))
				.andExpect(jsonPath("$.timestamp").isString());
	}

	@Test
	void aProtectedRouteWithAValidTokenReturnsTheCurrentUser() throws Exception {
		register("ash", "ash@example.com", "Pikachu25");
		String token = tokenOf(login("ash", "Pikachu25"));

		mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("ash"))
				.andExpect(jsonPath("$.role").value("USER"));
	}

	@Test
	void anExpiredTokenReturns401() throws Exception {
		Instant twoHoursAgo = Instant.now().minus(Duration.ofHours(2));
		String expired = new JwtTokenIssuer(JwtKeys.encoder(JwtKeys.fromBase64(SECRET)), Duration.ofHours(1),
				Clock.fixed(twoHoursAgo, ZoneOffset.UTC))
				.issue(new User(1L, "ash", "ash@example.com", "hash", Role.USER)).value();

		mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Your session has expired or is invalid, please log in again"));
	}

	@Test
	void aForgedTokenReturns401() throws Exception {
		String forged = new JwtTokenIssuer(JwtKeys.encoder(JwtKeys.generate()), Duration.ofHours(1), Clock.systemUTC())
				.issue(new User(1L, "admin", "admin@example.com", "hash", Role.ADMIN)).value();

		mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void publicRoutesNeedNoToken() throws Exception {
		Mockito.when(catalog.listPokemon(new PageRequest(0, 20))).thenReturn(new Page<>(List.of(), 0, 20, 0));

		mockMvc.perform(get("/api/pokemon")).andExpect(status().isOk());
		mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
	}

	@Test
	void anUnknownApiRouteRequiresAuthenticationFirst() throws Exception {
		// Routes are closed by default: only the ones listed as public are open.
		mockMvc.perform(get("/api/something-else"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value(not(containsString("Exception"))));
	}

	// --- helpers ----------------------------------------------------------------------------------

	private ResultActions register(String username, String email,
			String password) throws Exception {
		return mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "%s", "email": "%s", "password": "%s"}""".formatted(username, email, password)));
	}

	private ResultActions login(String username, String password)
			throws Exception {
		return mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"username": "%s", "password": "%s"}""".formatted(username, password)));
	}

	private static String tokenOf(ResultActions login) throws Exception {
		return JsonPath.read(login.andReturn().getResponse().getContentAsString(), "$.token");
	}
}
