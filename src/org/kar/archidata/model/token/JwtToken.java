package org.kar.archidata.model.token;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class JwtToken {
	@NotNull
	@Valid
	public JwtHeader header;
	@NotNull
	@Valid
	public JwtPayload payload;
	@NotNull
	public String signature;
}
