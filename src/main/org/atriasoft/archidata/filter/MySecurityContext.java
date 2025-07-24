package org.atriasoft.archidata.filter;

import java.security.Principal;
import java.util.Set;

import org.atriasoft.archidata.model.UserByToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.SecurityContext;

// https://simplapi.wordpress.com/2015/09/19/jersey-jax-rs-securitycontext-in-action/
public class MySecurityContext implements SecurityContext {
	private static final Logger LOGGER = LoggerFactory.getLogger(MySecurityContext.class);

	private final GenericContext contextPrincipale;
	private final String sheme;

	public MySecurityContext(final UserByToken userByToken, final String sheme) {
		this.contextPrincipale = new GenericContext(userByToken);
		this.sheme = sheme;
	}

	@Override
	public Principal getUserPrincipal() {
		return this.contextPrincipale;
	}

	public PartRight getRightOfRoleInGroup(final String group, final String role) {
		if (this.contextPrincipale.userByToken != null) {
			return this.contextPrincipale.userByToken.getRight(group, role);
		}
		return null;
	}

	public Set<String> getGroups() {
		if (this.contextPrincipale.userByToken != null) {
			return this.contextPrincipale.userByToken.getGroups();
		}
		return Set.of();
	}

	public boolean groupExist(final String group) {
		if (this.contextPrincipale.userByToken != null) {
			return this.contextPrincipale.userByToken.groupExist(group);
		}
		return false;
	}

	// Not sure the Long type is definitive.
	public Object getUserID() {
		if (this.contextPrincipale.userByToken != null) {
			return this.contextPrincipale.userByToken.id;
		}
		return null;
	}

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
					LOGGER.error("Fail to extract role of '{}'", role);
					ex.printStackTrace();
					return false;
				}
				needWrite = true;
			} else if (roleEdit.endsWith(":r")) {
				try {
					roleEdit = roleEdit.substring(0, roleEdit.length() - 2);
				} catch (final IndexOutOfBoundsException ex) {
					LOGGER.error("Fail to extract role of '{}'", role);
					ex.printStackTrace();
					return false;
				}
				needRead = true;
			} else if (roleEdit.endsWith(":rw")) {
				try {
					roleEdit = roleEdit.substring(0, roleEdit.length() - 3);
				} catch (final IndexOutOfBoundsException ex) {
					LOGGER.error("Fail to extract role of '{}'", role);
					ex.printStackTrace();
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

	public Object getRole(final String role) {
		LOGGER.info("contextPrincipale={}", this.contextPrincipale);
		if (this.contextPrincipale.userByToken != null) {
			LOGGER.info("contextPrincipale.userByToken={}", this.contextPrincipale.userByToken);
			LOGGER.info("contextPrincipale.userByToken.right={}", this.contextPrincipale.userByToken.right);
			return this.contextPrincipale.userByToken.right.get(role);
		}
		return null;
	}

	@Override
	public boolean isSecure() {
		return "https".equalsIgnoreCase(this.sheme);
	}

	@Override
	public String getAuthenticationScheme() {
		if (this.contextPrincipale.userByToken != null) {
			return "Bearer";
		}
		return null;
	}

}