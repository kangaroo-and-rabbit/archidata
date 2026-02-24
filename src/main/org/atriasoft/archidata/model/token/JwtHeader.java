package org.atriasoft.archidata.model.token;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record JwtHeader(
		@Size(max = 128) @NotNull String typ,
		@Size(max = 128) @NotNull String alg) {}
