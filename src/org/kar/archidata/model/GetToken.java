package org.kar.archidata.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public final class GetToken {
	public final String jwt;

	public GetToken(String jwt) {
		super();
		this.jwt = jwt;
	}
	
}
