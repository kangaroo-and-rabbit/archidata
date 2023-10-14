package org.kar.archidata.annotation.security;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

@NameBinding
@Retention(RUNTIME)
@Target({ METHOD })
public @interface RolesAllowed {
	String[] value();
}
