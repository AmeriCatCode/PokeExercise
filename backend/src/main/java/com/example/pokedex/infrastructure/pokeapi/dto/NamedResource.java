package com.example.pokedex.infrastructure.pokeapi.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** PokeAPI's reference to another resource: a name plus its URL. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NamedResource(String name, String url) {
}
