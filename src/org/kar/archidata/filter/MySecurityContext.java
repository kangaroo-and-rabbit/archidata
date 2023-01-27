package org.kar.archidata.filter;


import org.kar.archidata.model.User;
import org.kar.archidata.model.UserByToken;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

// https://simplapi.wordpress.com/2015/09/19/jersey-jax-rs-securitycontext-in-action/
class MySecurityContext implements SecurityContext {

    private final GenericContext contextPrincipale;
    private final String sheme;

    public MySecurityContext(User user, UserByToken userByToken, String sheme) {
        this.contextPrincipale = new GenericContext(user, userByToken);
        this.sheme = sheme;
    }

    @Override
    public Principal getUserPrincipal() {
        return contextPrincipale;
    }

    @Override
    public boolean isUserInRole(String role) {
    	if (contextPrincipale.user != null) {
	        if (role.contentEquals("ADMIN")) {
	            return contextPrincipale.user.admin == true;
	        }
	        if (role.contentEquals("USER")) {
	        	// if not an admin, this is a user...
	            return true; //contextPrincipale.user.admin == false;
	        }
        }
    	if (contextPrincipale.userByToken != null) {
    		Boolean value = this.contextPrincipale.userByToken.right.get(role);
    		return value == true;
    	}
        return false;
    }

    @Override
    public boolean isSecure() {
        return sheme.equalsIgnoreCase("https");
    }

    @Override
    public String getAuthenticationScheme() {
    	if (contextPrincipale.user != null) {
    		return "Yota";
    	}
    	if (contextPrincipale.userByToken != null) {
    		return "Zota";
    	}
        return null;
    }

}