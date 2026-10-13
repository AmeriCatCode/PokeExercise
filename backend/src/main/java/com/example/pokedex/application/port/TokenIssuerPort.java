package com.example.pokedex.application.port;

import com.example.pokedex.domain.user.IssuedToken;
import com.example.pokedex.domain.user.User;

/**
 * Creates the login token a user sends on protected requests.
 */
public interface TokenIssuerPort {

	IssuedToken issue(User user);
}
