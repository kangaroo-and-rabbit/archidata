package org.atriasoft.archidata.model;

public record Token(
		Long id,
		Long userId,
		String token,
		String createTime,
		String endValidityTime) {
}
