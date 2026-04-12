package org.atriasoft.archidata.model.token;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

/**
 * Represents the payload portion of a JSON Web Token (JWT), containing claims such as subject, issuer, roles, rights, and expiration.
 *
 * @param sub the subject (user identification)
 * @param application the target application identifier
 * @param iss the issuer of the token
 * @param roles the roles map, structured as {@code Map<application, Map<roleName, accessLevel>>}
 * @param right the fine-grained rights map, structured as {@code Map<application, Map<rightName, accessLevel>>}
 * @param login the user login name
 * @param exp the expiration timestamp (seconds since epoch)
 * @param iat the issued-at timestamp (seconds since epoch)
 */
public record JwtPayload(
		// User identification
		@NotNull String sub,
		// Application destination
		@NotNull String application,
		// Emitter of the token
		@NotNull String iss,
		// Roles Map<application, Map<roleName, accessLevel>>
		@NotNull Map<@NotNull String, Map<@NotNull String, @NotNull Long>> roles,
		// Fine-grained Rights Map<application, Map<rightName, accessLevel>>
		@NotNull Map<@NotNull String, Map<@NotNull String, @NotNull Long>> right,
		// user name
		@NotNull String login,
		// Expiration (timestamp)
		@NotNull Long exp,
		// Create time (timestamp)
		@NotNull Long iat) {}
