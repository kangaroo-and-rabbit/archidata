package org.atriasoft.archidata.annotation.security;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/**
 * JAX-RS name-binding annotation that permits authentication tokens to be passed in the URI.
 *
 * <p>When applied to a resource method, the authentication filter will accept
 * the JWT token from a query parameter in addition to the standard Authorization header.
 */
@NameBinding
@Retention(RUNTIME)
@Target({ METHOD })
public @interface PermitTokenInURI {}
