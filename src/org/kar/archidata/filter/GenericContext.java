package org.kar.archidata.filter;

import org.kar.archidata.model.User;
import org.kar.archidata.model.UserByToken;

import java.security.Principal;

public class GenericContext implements Principal {

    public User user;
    public UserByToken userByToken;

    public GenericContext(User user, UserByToken userByToken) {
        this.user = user;
        this.userByToken = userByToken;
    }

    @Override
    public String getName() {
        if (user == null) {
            return "???";
        }
        return user.login;
    }
}
