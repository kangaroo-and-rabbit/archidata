package org.atriasoft.archidata.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents an authentication token with its metadata.
 *
 * @param id unique identifier of the token
 * @param userId identifier of the user who owns this token
 * @param token the token string value
 * @param createTime timestamp when the token was created
 * @param endValidityTime timestamp when the token expires
 *
 * @deprecated not used by archidata: authentication reads {@link UserByToken}. Define the record in your application.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Deprecated(since = "0.55.0", forRemoval = true)
public record Token(
		Long id,
		Long userId,
		String token,
		String createTime,
		String endValidityTime) {

}
