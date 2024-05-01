package org.kar.archidata.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class GetToken {
	@Column(length = -1, nullable = false)
	public String jwt;

	public GetToken(final String jwt) {
		this.jwt = jwt;
	}

}
