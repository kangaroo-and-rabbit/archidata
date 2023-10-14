package org.kar.archidata.filter;

import java.security.Principal;

import org.kar.archidata.model.UserByToken;

import jakarta.ws.rs.core.SecurityContext;

// https://simplapi.wordpress.com/2015/09/19/jersey-jax-rs-securitycontext-in-action/
class MySecurityContext implements SecurityContext {
	
	private final GenericContext contextPrincipale;
	private final String sheme;
	
	public MySecurityContext(UserByToken userByToken, String sheme) {
		this.contextPrincipale = new GenericContext(userByToken);
		this.sheme = sheme;
	}
	
	@Override
	public Principal getUserPrincipal() {
		return contextPrincipale;
	}
	
	@Override
	public boolean isUserInRole(String role) {
		if (contextPrincipale.userByToken != null) {
			Object value = this.contextPrincipale.userByToken.right.get(role);
			if (value instanceof Boolean ret) {
				return ret;
			}
		}
		return false;
	}
	
	@Override
	public boolean isSecure() {
		return sheme.equalsIgnoreCase("https");
	}
	
	@Override
	public String getAuthenticationScheme() {
		if (contextPrincipale.userByToken != null) {
			return "Zota";
		}
		return null;
	}
	
}