package org.kar.archidata.filter;

import java.security.Principal;
import java.util.Set;

import org.kar.archidata.model.UserByToken;
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

	public boolean isUserInRole(final String group, final String role) {
		if (this.contextPrincipale.userByToken != null) {
			final Object value = this.contextPrincipale.userByToken.getRight(group, role);
			if (value instanceof final Boolean ret) {
				return ret;
			}
		}
		return false;
	}

	public Object getUserInRole(final String group, final String role) {
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

	@Override
	public boolean isUserInRole(final String role) {
		// TODO Auto-generated method stub
		return isUserInRole("???", role);
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