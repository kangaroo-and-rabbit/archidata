package org.atriasoft.archidata.model.token;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

/**
 * Represents the payload portion of a JSON Web Token (JWT), containing claims such as subject, issuer, roles, rights, and expiration.
 *
 * @param sub the subject (user identification)
 * @param application the target application identifier
 * @param iss the issuer of the token
 * @param roles the roles map, structured as {@code Map<application, Map<roleName, accessLevel>>} (optional)
 * @param right the fine-grained rights map, structured as {@code Map<application, Map<rightName, accessLevel>>} (optional)
 * @param login the user login name
 * @param exp the expiration timestamp (seconds since epoch)
 * @param iat the issued-at timestamp (seconds since epoch)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JwtPayload(
		// User identification
		@NotNull String sub,
		// Application destination
		@NotNull String application,
		// Emitter of the token
		@NotNull String iss,
		// Roles Map<application, Map<roleName, accessLevel>> (optional, at least one of roles/right must be present)
		@Nullable Map<@NotNull String, Map<@NotNull String, @NotNull Long>> roles,
		// Fine-grained Rights Map<application, Map<rightName, accessLevel>> (optional, at least one of roles/right must be present)
		@Nullable Map<@NotNull String, Map<@NotNull String, @NotNull Long>> right,
		// user name
		@NotNull String login,
		// Expiration (timestamp)
		@NotNull Long exp,
		// Create time (timestamp)
		@NotNull Long iat) {

	/**
	 * Compact constructor copying the roles and the rights into unmodifiable maps.
	 */
	public JwtPayload {
		roles = unmodifiableCopy(roles);
		right = unmodifiableCopy(right);
	}

	/**
	 * The roles, structured as {@code Map<application, Map<roleName, accessLevel>>}.
	 *
	 * @return the unmodifiable roles map, or {@code null} if the token has none
	 */
	@Override
	public @Nullable Map<@NotNull String, Map<@NotNull String, @NotNull Long>> roles() {
		return this.roles == null ? null : Collections.unmodifiableMap(this.roles);
	}

	/**
	 * The fine-grained rights, structured as {@code Map<application, Map<rightName, accessLevel>>}.
	 *
	 * @return the unmodifiable rights map, or {@code null} if the token has none
	 */
	@Override
	public @Nullable Map<@NotNull String, Map<@NotNull String, @NotNull Long>> right() {
		return this.right == null ? null : Collections.unmodifiableMap(this.right);
	}

	private static Map<String, Map<String, Long>> unmodifiableCopy(final Map<String, Map<String, Long>> source) {
		if (source == null) {
			return null;
		}
		final Map<String, Map<String, Long>> out = new LinkedHashMap<>();
		for (final Map.Entry<String, Map<String, Long>> entry : source.entrySet()) {
			final Map<String, Long> group = entry.getValue();
			out.put(entry.getKey(), group == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(group)));
		}
		return Collections.unmodifiableMap(out);
	}
}
