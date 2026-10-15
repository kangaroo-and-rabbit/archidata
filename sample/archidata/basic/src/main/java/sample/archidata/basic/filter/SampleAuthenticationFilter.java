package sample.archidata.basic.filter;

import org.atriasoft.archidata.filter.AuthenticationFilter;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.ext.Provider;

/**
 * Authentication filter of the application.
 *
 * <p>The archidata base class does all the work: it reads the {@code Authorization: Bearer <JWT>}
 * header, checks the signature with the public key held by {@code JWTWrapper}, and enforces
 * {@code @PermitAll} / {@code @RolesAllowed} / {@code @RightAllowed}. The only thing to provide is
 * the application name: it selects the group of roles read in the token
 * ({@code {"roles": {"sample": {"ADMIN": 3}}}}).
 *
 * <p>Override {@code validateToken(String)} to also accept {@code ApiKey} tokens (see karideo's
 * worker authentication for an example).
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class SampleAuthenticationFilter extends AuthenticationFilter {
	/** Name of the application, i.e. the key of its roles in the JWT. */
	public static final String APPLICATION_NAME = "sample";

	public SampleAuthenticationFilter() {
		// The second argument (expected JWT issuer) defaults to "Karso".
		super(APPLICATION_NAME);
	}
}
