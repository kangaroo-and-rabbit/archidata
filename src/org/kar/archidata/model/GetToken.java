package org.kar.archidata.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public final class GetToken {
	public final String jwt;

	public GetToken(String jwt) {
		super();
		this.jwt = jwt;
	}
	
}
