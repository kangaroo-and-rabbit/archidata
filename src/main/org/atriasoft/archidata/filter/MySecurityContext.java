package org.atriasoft.archidata.filter;

import java.security.Principal;
import java.util.Set;

import org.atriasoft.archidata.model.UserByToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.SecurityContext;

/**
 * Custom {@link SecurityContext} implementation that provides role-based access control
 * using {@link UserByToken} information extracted from authentication tokens.
 *
 * <p>
 * Supports role checking with optional read/write permission suffixes (e.g., {@code "role:r"},
 * {@code "role:w"}, {@code "role:rw"}) and group-based role resolution.
 * </p>
 */
// https://simplapi.wordpress.com/2015/09/19/jersey-jax-rs-securitycontext-in-action/
public class MySecurityContext implements SecurityContext {
	private static final Logger LOGGER = LoggerFactory.getLogger(MySecurityContext.class);

	private final GenericContext contextPrincipale;
	private final String sheme;

	/**
	 * Constructs a security context from the authenticated user token and request scheme.
	 *
	 * @param userByToken the authenticated user's token data
	 * @param sheme the URI scheme of the request (e.g., "https")
	 */
	public MySecurityContext(final UserByToken userByToken, final String sheme) {
		this.contextPrincipale = new GenericContext(userByToken);
		this.sheme = sheme;
	}

	/**
	 * Returns the principal associated with this security context.
	 *
	 * @return the {@link GenericContext} principal
	 */
	@Override
	public Principal getUserPrincipal() {
		return this.contextPrincipale;
	}

	/**
	 * Returns the access right for a specific role within a group.
	 *
	 * @param group the group name
	 * @param role the role name within the group
	 * @return the {@link PartRight} for the role, or {@code null} if the user has no roles
	 */
	public PartRight getRightOfRoleInGroup(final String group, final String role) {
		if (this.contextPrincipale.userByToken != null) {
			return this.contextPrincipale.userByToken.getRoleForKey(group, role);
		}
		return null;
	}

	/**
	 * Returns the set of groups the authenticated user belongs to.
	 *
	 * @return the set of group names, or an empty set if no user is available
	 */
	public Set<String> getGroups() {
		if (this.contextPrincipale.userByToken != null) {
			return this.contextPrincipale.userByToken.getGroups();
		}
		return Set.of();
	}

	/**
	 * Checks whether a specific group exists for the authenticated user.
	 *
	 * @param group the group name to check
	 * @return {@code true} if the group exists for the user
	 */
	public boolean groupExist(final String group) {
		if (this.contextPrincipale.userByToken != null) {
			return this.contextPrincipale.userByToken.groupExist(group);
		}
		return false;
	}

	/**
	 * Returns the unique identifier of the authenticated user.
	 *
	 * @return the user ID, or {@code null} if no user is available
	 */
	// Not sure the Long type is definitive.
	public Object getUserID() {
		if (this.contextPrincipale.userByToken != null) {
			return this.contextPrincipale.userByToken.getOid();
		}
		return null;
	}

	/**
	 * Checks whether the user has the required read/write permissions for a role within a group.
	 *
	 * @param group the group name
	 * @param role the role name
	 * @param needRead whether read permission is required
	 * @param needWrite whether write permission is required
	 * @return {@code true} if the user has the required permissions
	 */
	public boolean checkRightInGroup(
			final String group,
			final String role,
			final boolean needRead,
			final boolean needWrite) {
		if ("USER".equals(role)) {
			if (groupExist(group)) {
				return true;
			}
			return false;
		}
		// get associated Roles:
		final PartRight rightPart = getRightOfRoleInGroup(group, role);
		if (PartRight.READ_WRITE.equals(rightPart)) {
			return true;
		}
		if (!needRead && needWrite && PartRight.WRITE.equals(rightPart)) {
			return true;
		}
		if (needRead && !needWrite && PartRight.READ.equals(rightPart)) {
			return true;
		}
		return false;
	}

	/**
	 * Checks whether the authenticated user has the specified role.
	 *
	 * <p>
	 * The role string can include permission suffixes ({@code :r}, {@code :w}, {@code :rw})
	 * and group prefixes separated by {@code /} (e.g., {@code "group/role:rw"}).
	 * </p>
	 *
	 * @param role the role string to check
	 * @return {@code true} if the user has the specified role with required permissions
	 */
	@Override
	public boolean isUserInRole(final String role) {
		String roleEdit = role;
		boolean needRead = false;
		boolean needWrite = false;
		// Check if the API overwrite the right needed for this API.
		if (roleEdit.contains(":")) {
			if (roleEdit.endsWith(":w")) {
				try {
					roleEdit = roleEdit.substring(0, roleEdit.length() - 2);
				} catch (final IndexOutOfBoundsException ex) {
					LOGGER.error("Fail to extract role of '{}': {}", role, ex.getMessage(), ex);
					return false;
				}
				needWrite = true;
			} else if (roleEdit.endsWith(":r")) {
				try {
					roleEdit = roleEdit.substring(0, roleEdit.length() - 2);
				} catch (final IndexOutOfBoundsException ex) {
					LOGGER.error("Fail to extract role of '{}': {}", role, ex.getMessage(), ex);
					return false;
				}
				needRead = true;
			} else if (roleEdit.endsWith(":rw")) {
				try {
					roleEdit = roleEdit.substring(0, roleEdit.length() - 3);
				} catch (final IndexOutOfBoundsException ex) {
					LOGGER.error("Fail to extract role of '{}': {}", role, ex.getMessage(), ex);
					return false;
				}
				needRead = true;
				needWrite = true;
			} else {
				LOGGER.error("Request check right of an unknow right mode: {} (after ':')", roleEdit);
				return false;
			}
		}
		if (roleEdit.contains("/")) {
			final String[] elements = roleEdit.split("/");
			return checkRightInGroup(elements[0], elements[1], needRead, needWrite);
		}
		// Special case, if the token is valid, it is an USER ...
		if ("USER".equals(roleEdit)) {
			return true;
		}
		return checkRightInGroup("?system?", roleEdit, needRead, needWrite);
	}

	/**
	 * Returns the roles map associated with the given group name.
	 *
	 * @param group the group name to look up
	 * @return the roles map for the group, or {@code null} if no user or roles are available
	 */
	public Object getRole(final String group) {
		LOGGER.info("contextPrincipale={}", this.contextPrincipale);
		if (this.contextPrincipale.userByToken != null) {
			LOGGER.info("contextPrincipale.userByToken={}", this.contextPrincipale.userByToken);
			LOGGER.info("contextPrincipale.userByToken.roles={}", this.contextPrincipale.userByToken.getRoles());
			return this.contextPrincipale.userByToken.getRoles().get(group);
		}
		return null;
	}

	/**
	 * Checks whether the authenticated user has the required access level for a fine-grained right.
	 * Uses bitmask logic: {@code (userRight &amp; required) == required}.
	 *
	 * @param applicationName the application name (group)
	 * @param rightName the right name to check (e.g., "articles", "users")
	 * @param requiredAccess the required {@link PartRight} access level
	 * @return {@code true} if the user has the required access level
	 */
	public boolean hasResourceRight(
			final String applicationName,
			final String rightName,
			final PartRight requiredAccess) {
		if (this.contextPrincipale.userByToken == null) {
			return false;
		}
		final PartRight userRight = this.contextPrincipale.userByToken.getRightForKey(applicationName, rightName);
		if (userRight == null) {
			return false;
		}
		return (userRight.getValue() & requiredAccess.getValue()) == requiredAccess.getValue();
	}

	/**
	 * Returns the fine-grained right value for a specific application and right name.
	 *
	 * @param applicationName the application name (group)
	 * @param rightName the right name to look up
	 * @return the {@link PartRight} for the right, or {@code null} if not found
	 */
	public PartRight getResourceRight(final String applicationName, final String rightName) {
		if (this.contextPrincipale.userByToken != null) {
			return this.contextPrincipale.userByToken.getRightForKey(applicationName, rightName);
		}
		return null;
	}

	/**
	 * Returns whether the request was made using a secure channel (HTTPS).
	 *
	 * @return {@code true} if the request scheme is HTTPS
	 */
	@Override
	public boolean isSecure() {
		return "https".equalsIgnoreCase(this.sheme);
	}

	/**
	 * Returns the authentication scheme used for this request.
	 *
	 * @return "Bearer" if a user is authenticated, or {@code null} otherwise
	 */
	@Override
	public String getAuthenticationScheme() {
		if (this.contextPrincipale.userByToken != null) {
			return "Bearer";
		}
		return null;
	}

}
