package com.example.pokedex.web.auth;

import com.example.pokedex.application.usecase.LoginUseCase;
import com.example.pokedex.application.usecase.RegisterUserUseCase;
import com.example.pokedex.web.auth.AuthDtos.CurrentUserResponse;
import com.example.pokedex.web.auth.AuthDtos.LoginRequest;
import com.example.pokedex.web.auth.AuthDtos.LoginResponse;
import com.example.pokedex.web.auth.AuthDtos.RegisterRequest;
import com.example.pokedex.web.auth.AuthDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registration and login (public), and the current user (protected).
 * Logging out is done by the client, which deletes its token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final RegisterUserUseCase registerUser;
	private final LoginUseCase login;

	public AuthController(RegisterUserUseCase registerUser, LoginUseCase login) {
		this.registerUser = registerUser;
		this.login = login;
	}

	@Operation(summary = "Create an account")
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse register(@Valid @RequestBody RegisterRequest request) {
		return UserResponse.from(registerUser.register(request.username(), request.email(), request.password()));
	}

	@Operation(summary = "Log in and get a token for the protected routes")
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		LoginUseCase.LoginResult result = login.login(request.username(), request.password());
		return new LoginResponse(result.token().value(), "Bearer", result.token().expiresAt(),
				UserResponse.from(result.user()));
	}

	@Operation(summary = "The logged-in user (requires a token)")
	@GetMapping("/me")
	public CurrentUserResponse me(@AuthenticationPrincipal Jwt jwt) {
		Number id = jwt.getClaim("uid");
		return new CurrentUserResponse(id == null ? null : id.longValue(), jwt.getSubject(),
				jwt.getClaimAsString("role"));
	}
}
