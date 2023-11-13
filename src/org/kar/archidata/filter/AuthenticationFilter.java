package org.kar.archidata.filter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
// https://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey
// https://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey/45814178#45814178
// https://stackoverflow.com/questions/32817210/how-to-access-jersey-resource-secured-by-rolesallowed

import org.kar.archidata.annotation.security.PermitTokenInURI;
import org.kar.archidata.catcher.RestErrorResponse;
import org.kar.archidata.model.UserByToken;
import org.kar.archidata.util.JWTWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jwt.JWTClaimsSet;

import jakarta.annotation.Priority;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

//@PreMatching
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
	final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);
	@Context
	private ResourceInfo resourceInfo;
	protected final String applicationName;

	private static final String AUTHENTICATION_SCHEME = "Yota";
	private static final String AUTHENTICATION_TOKEN_SCHEME = "Zota";

	public AuthenticationFilter(final String applicationName) {
		this.applicationName = applicationName;
	}

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {
		/* logger.debug("-----------------------------------------------------"); logger.debug("----          Check if have authorization        ----");
		 * logger.debug("-----------------------------------------------------"); logger.debug("   for:{}", requestContext.getUriInfo().getPath()); */
		final Method method = this.resourceInfo.getResourceMethod();
		// Access denied for all
		if (method.isAnnotationPresent(DenyAll.class)) {
			this.logger.debug("   ==> deny all {}", requestContext.getUriInfo().getPath());
			requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).entity("Access blocked !!!").build());
			return;
		}

		// Access allowed for all
		if (method.isAnnotationPresent(PermitAll.class)) {
			// logger.debug(" ==> permit all " + requestContext.getUriInfo().getPath());
			// no control ...
			return;
		}
		// this is a security guard, all the API must define their access level:
		if (!method.isAnnotationPresent(RolesAllowed.class)) {
			this.logger.error("   ==> missing @RolesAllowed {}", requestContext.getUriInfo().getPath());
			requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).entity("Access ILLEGAL !!!").build());
			return;
		}

		// Get the Authorization header from the request
		String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
		// logger.debug("authorizationHeader: {}", authorizationHeader);
		if (authorizationHeader == null && method.isAnnotationPresent(PermitTokenInURI.class)) {
			final MultivaluedMap<String, String> quaryparam = requestContext.getUriInfo().getQueryParameters();
			for (final Entry<String, List<String>> item : quaryparam.entrySet()) {
				if (item.getKey().equals(HttpHeaders.AUTHORIZATION)) {
					if (!item.getValue().isEmpty()) {
						authorizationHeader = item.getValue().get(0);
					}
					break;
				}
			}
		}
		// logger.debug("authorizationHeader: {}", authorizationHeader);
		final boolean isApplicationToken = isApplicationTokenBasedAuthentication(authorizationHeader);
		final boolean isJwtToken = isTokenBasedAuthentication(authorizationHeader);
		// Validate the Authorization header data Model "Yota jwt.to.ken" "Zota tokenId:hash(token)"
		if (!isApplicationToken && !isJwtToken) {
			this.logger.warn("REJECTED unauthorized: {}", requestContext.getUriInfo().getPath());
			abortWithUnauthorized(requestContext, "REJECTED unauthorized: " + requestContext.getUriInfo().getPath());
			return;
		}
		UserByToken userByToken = null;
		if (isJwtToken) {
			// Extract the token from the Authorization header (Remove "Yota ")
			final String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();
			// logger.debug("token: {}", token);
			try {
				userByToken = validateJwtToken(token);
			} catch (final Exception e) {
				this.logger.error("Fail to validate token: {}", e.getMessage());
				abortWithUnauthorized(requestContext, "Fail to validate token: " + e.getMessage());
				return;
			}
			if (userByToken == null) {
				this.logger.warn("get a NULL user ...");
				abortWithUnauthorized(requestContext, "get a NULL user ...");
				return;
			}
		} else {
			// Extract the token from the Authorization header (Remove "Zota ")
			final String token = authorizationHeader.substring(AUTHENTICATION_TOKEN_SCHEME.length()).trim();
			// logger.debug("token: {}", token);
			try {
				userByToken = validateToken(token);
			} catch (final Exception e) {
				this.logger.error("Fail to validate token: {}", e.getMessage());
				abortWithUnauthorized(requestContext, "Fail to validate token: " + e.getMessage());
				return;
			}
			if (userByToken == null) {
				this.logger.warn("get a NULL application ...");
				abortWithUnauthorized(requestContext, "get a NULL application ...");
				return;
			}

		}
		// create the security context model:
		final String scheme = requestContext.getUriInfo().getRequestUri().getScheme();
		final MySecurityContext userContext = new MySecurityContext(userByToken, scheme);
		// retrieve the allowed right:
		final RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
		final List<String> roles = Arrays.asList(rolesAnnotation.value());
		// check if the user have the right:
		boolean haveRight = false;
		for (final String role : roles) {
			if (userContext.isUserInRole(role)) {
				haveRight = true;
				break;
			}
		}
		// Is user valid?
		if (!haveRight) {
			this.logger.error("REJECTED not enought right : {} require: {}", requestContext.getUriInfo().getPath(), roles);
			requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Not enought RIGHT !!!").build());
			return;
		}
		requestContext.setSecurityContext(userContext);
		// logger.debug("Get local user : {} / {}", user, userByToken);
	}

	private boolean isTokenBasedAuthentication(final String authorizationHeader) {
		// Check if the Authorization header is valid
		// It must not be null and must be prefixed with "Bearer" plus a whitespace
		// The authentication scheme comparison must be case-insensitive
		return authorizationHeader != null && authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
	}

	private boolean isApplicationTokenBasedAuthentication(final String authorizationHeader) {
		// Check if the Authorization header is valid
		// It must not be null and must be prefixed with "Bearer" plus a whitespace
		// The authentication scheme comparison must be case-insensitive
		return authorizationHeader != null && authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_TOKEN_SCHEME.toLowerCase() + " ");
	}

	private void abortWithUnauthorized(final ContainerRequestContext requestContext, final String message) {

		// Abort the filter chain with a 401 status code response
		// The WWW-Authenticate header is sent along with the response
		this.logger.warn("abortWithUnauthorized:");
		final RestErrorResponse ret = new RestErrorResponse(Response.Status.UNAUTHORIZED, "Unauthorized", message);
		this.logger.error("Error UUID={}", ret.uuid);
		requestContext.abortWith(Response.status(ret.status).header(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME + " base64(HEADER).base64(CONTENT).base64(KEY)").entity(ret)
				.type(MediaType.APPLICATION_JSON).build());
	}

	protected UserByToken validateToken(final String authorization) throws Exception {
		this.logger.info("Must be Override by the application implmentation, otherwise it dose not work");
		return null;
	}

	// must be override to be good implementation
	protected UserByToken validateJwtToken(final String authorization) throws Exception {
		// logger.debug(" validate token : " + authorization);
		final JWTClaimsSet ret = JWTWrapper.validateToken(authorization, "KarAuth", null);
		// check the token is valid !!! (signed and coherent issuer...
		if (ret == null) {
			this.logger.error("The token is not valid: '{}'", authorization);
			return null;
		}
		// check userID
		final String userUID = ret.getSubject();
		final long id = Long.parseLong(userUID);
		final UserByToken user = new UserByToken();
		user.id = id;
		user.name = (String) ret.getClaim("login");
		user.type = UserByToken.TYPE_USER;
		final Object rowRight = ret.getClaim("right");
		if (rowRight != null) {
			final Map<String, Map<String, Object>> rights = (Map<String, Map<String, Object>>) ret.getClaim("right");
			if (rights.containsKey(this.applicationName)) {
				user.right = rights.get(this.applicationName);
			} else {
				this.logger.error("Connect with no right for this application='{}' full Right='{}'", this.applicationName, rights);
			}
		}
		// logger.debug("request user: '{}' right: '{}' row='{}'", userUID, user.right, rowRight);
		return user;
		// return UserDB.getUserOrCreate(id, (String)ret.getClaim("login") );
	}
}
