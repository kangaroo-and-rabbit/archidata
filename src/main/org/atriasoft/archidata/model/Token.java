package org.atriasoft.archidata.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Token(
		Long id,
		Long userId,
		String token,
		String createTime,
		String endValidityTime) {

}
