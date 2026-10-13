package com.example.pokedex.infrastructure.persistence;

import com.example.pokedex.domain.user.Role;
import com.example.pokedex.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_user")
class UserEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String username;

	@Column(nullable = false, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 100)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected UserEntity() {
		// for JPA
	}

	static UserEntity from(User user) {
		UserEntity entity = new UserEntity();
		entity.id = user.id();
		entity.username = user.username();
		entity.email = user.email();
		entity.passwordHash = user.passwordHash();
		entity.role = user.role();
		entity.createdAt = Instant.now();
		return entity;
	}

	User toDomain() {
		return new User(id, username, email, passwordHash, role);
	}
}
