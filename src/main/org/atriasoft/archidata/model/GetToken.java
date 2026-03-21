package org.atriasoft.archidata.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a request to obtain a token, containing the JWT string.
 *
 * @param jwt the JSON Web Token string
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetToken(
		@Column(length = -1, nullable = false) @NotNull String jwt) {}
