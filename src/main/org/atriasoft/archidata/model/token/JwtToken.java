package org.atriasoft.archidata.model.token;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record JwtToken(
		@NotNull @Valid JwtHeader header,
		@NotNull @Valid JwtPayload payload,
		@NotNull String signature) {
}
