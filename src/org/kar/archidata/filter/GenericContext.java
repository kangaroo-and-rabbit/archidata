package org.kar.archidata.filter;

import org.kar.archidata.model.UserByToken;

import java.security.Principal;

public class GenericContext implements Principal {

    public UserByToken userByToken;

    public GenericContext(UserByToken userByToken) {
        this.userByToken = userByToken;
    }

    @Override
    public String getName() {
        if (this.userByToken == null) {
            return "???";
        }
        return this.userByToken.name;
    }
}
