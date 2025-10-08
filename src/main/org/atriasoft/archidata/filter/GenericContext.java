package org.atriasoft.archidata.filter;

import java.security.Principal;

import org.atriasoft.archidata.model.UserByToken;

public class GenericContext implements Principal {

	public UserByToken userByToken;

	public GenericContext(final UserByToken userByToken) {
		this.userByToken = userByToken;
	}

	@Override
	public String getName() {
		if (this.userByToken == null) {
			return "???";
		}
		return this.userByToken.name;
	}

	public Object getUserID() {
		if (this.userByToken != null) {
			return this.userByToken.oid;
		}
		return null;
	}
}
