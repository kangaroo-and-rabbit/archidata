package org.kar.archidata.filter;

import java.security.Principal;

import org.kar.archidata.model.UserByToken;

import jakarta.ws.rs.core.SecurityContext;

// https://simplapi.wordpress.com/2015/09/19/jersey-jax-rs-securitycontext-in-action/
class MySecurityContext implements SecurityContext {
	
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
	
	@Override
	public boolean isUserInRole(final String role) {
		if (this.contextPrincipale.userByToken != null) {
			final Object value = this.contextPrincipale.userByToken.right.get(role);
			if (value instanceof final Boolean ret) {
				return ret;
			}
		}
		return false;
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