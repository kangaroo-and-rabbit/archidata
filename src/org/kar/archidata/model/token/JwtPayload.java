package org.kar.archidata.model.token;

import java.util.Map;

import jakarta.validation.constraints.NotNull;

public class JwtPayload {
	// User identification
	@NotNull
	public String sub;
	// Application destination
	@NotNull
	public String application;
	// Emitter of the token
	@NotNull
	public String iss;
	// Access Right Map<application, Map< section, right>>
	@NotNull
	public Map<String, Map<String, Long>> right;
	// user name
	@NotNull
	public String login;
	// Expiration (timestamp)
	@NotNull
	public Long exp;
	// Create time (timestamp)
	@NotNull
	public Long iat;
}
