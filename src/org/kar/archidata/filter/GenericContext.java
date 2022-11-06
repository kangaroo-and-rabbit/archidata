package org.kar.archidata.filter;

import org.kar.archidata.model.User;

import java.security.Principal;

public class GenericContext implements Principal {

    public User user;

    public GenericContext(User user) {
        this.user = user;
    }

    @Override
    public String getName() {
        if (user == null) {
            return "???";
        }
        return user.login;
    }
}
