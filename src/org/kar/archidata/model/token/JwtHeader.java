package org.kar.archidata.model.token;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class JwtHeader {
	@Size(max = 128)
	@NotNull
	public String typ;
	@Size(max = 128)
	@NotNull
	public String alg;
}
