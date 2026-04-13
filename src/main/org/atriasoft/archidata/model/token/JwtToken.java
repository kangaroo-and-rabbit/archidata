package org.atriasoft.archidata.model.token;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a complete JSON Web Token (JWT) consisting of a header, payload, and signature.
 *
 * @param header the JWT header containing type and algorithm
 * @param payload the JWT payload containing claims
 * @param signature the cryptographic signature string
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record JwtToken(
		@NotNull @Valid JwtHeader header,
		@NotNull @Valid JwtPayload payload,
		@NotNull String signature) {}
