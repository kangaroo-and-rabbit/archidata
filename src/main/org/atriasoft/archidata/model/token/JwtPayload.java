package org.atriasoft.archidata.model.token;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public record JwtPayload(
		// User identification
		@NotNull String sub,
		// Application destination
		@NotNull String application,
		// Emitter of the token
		@NotNull String iss,
		// Access Right Map<application, Map< section, right>>
		@NotNull Map<@NotNull String, Map<@NotNull String, @NotNull Long>> right,
		// user name
		@NotNull String login,
		// Expiration (timestamp)
		@NotNull Long exp,
		// Create time (timestamp)
		@NotNull Long iat) {}
