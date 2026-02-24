package org.atriasoft.archidata.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GetToken(
		@Column(length = -1, nullable = false) @NotNull String jwt) {}
