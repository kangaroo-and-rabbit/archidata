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
	private final String applicationName;

	/**
	 * Constructs a GenericContext with the given user token.
	 *
	 * @param userByToken the authenticated user's token data
	 */
	public GenericContext(final UserByToken userByToken) {
		this(userByToken, null);
	}

	/**
	 * Constructs a GenericContext that knows the application the request was authenticated for.
	 *
	 * @param userByToken the authenticated user's token data
	 * @param applicationName the application name of the {@link AuthenticationFilter}, {@code null} if there is none
	 */
	public GenericContext(final UserByToken userByToken, final String applicationName) {
		this.userByToken = userByToken;
		this.applicationName = applicationName;
	}

	/**
	 * Returns the application the request was authenticated for: the group a resource prefixes a role with to check it
	 * as {@link AuthenticationFilter} does (a role without group is looked for in the {@code ?system?} group). It lives
	 * here because a resource reaches this principal, whereas the {@code SecurityContext} it receives through
	 * {@code @Context} is a proxy, not a {@link MySecurityContext}.
	 *
	 * @return the application name, or {@code null} if this principal was built without one
	 */
	public String getApplicationName() {
		return this.applicationName;
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
