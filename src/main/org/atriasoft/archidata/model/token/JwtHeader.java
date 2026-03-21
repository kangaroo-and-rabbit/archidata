package org.atriasoft.archidata.model.token;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents the header portion of a JSON Web Token (JWT), containing the token type and signing algorithm.
 *
 * @param typ the token type (e.g. "JWT")
 * @param alg the signing algorithm (e.g. "RS256")
 */
public record JwtHeader(
		@Size(max = 128) @NotNull String typ,
		@Size(max = 128) @NotNull String alg) {}
