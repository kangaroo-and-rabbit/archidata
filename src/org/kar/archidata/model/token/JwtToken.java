package org.kar.archidata.model.token;

import jakarta.validation.constraints.NotNull;

public class JwtToken {
	@NotNull
	public JwtHeader header;
	@NotNull
	public JwtPayload payload;
	@NotNull
	public String signature;
}
