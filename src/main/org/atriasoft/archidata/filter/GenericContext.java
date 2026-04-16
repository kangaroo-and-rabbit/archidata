package org.atriasoft.archidata.filter;

import java.security.Principal;

import org.atriasoft.archidata.model.UserByToken;

/**
 * A {@link Principal} implementation that wraps a {@link UserByToken} to provide
 * identity information for the authenticated user.
 */
public class GenericContext implements Principal {

	/** The user token data associated with this principal. */
	public UserByToken userByToken;

	/**
	 * Constructs a GenericContext with the given user token.
	 *
	 * @param userByToken the authenticated user's token data
	 */
	public GenericContext(final UserByToken userByToken) {
		this.userByToken = userByToken;
	}

	/**
	 * Returns the name of the authenticated user.
	 *
	 * @return the user name, or "???" if no user is available
	 */
	@Override
	public String getName() {
		if (this.userByToken == null) {
			return "???";
		}
		return this.userByToken.getName();
	}

	/**
	 * Returns the unique identifier of the authenticated user.
	 *
	 * @return the user ID, or {@code null} if no user is available
	 */
	public Object getUserID() {
		if (this.userByToken != null) {
			return this.userByToken.getOid();
		}
		return null;
	}
}
