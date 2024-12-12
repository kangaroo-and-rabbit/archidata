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
import org.kar.archidata.exception.SystemException;
import org.kar.archidata.model.UserByToken;
import org.kar.archidata.tools.JWTWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nimbusds.jwt.JWTClaimsSet;

import jakarta.annotation.Priority;
import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Path;
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
	private final static Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);
	@Context
	private ResourceInfo resourceInfo;
	protected final String applicationName;
	protected final String issuer;

	public static final String AUTHENTICATION_SCHEME = "Bearer";
	public static final String APIKEY = "ApiKey";

	public AuthenticationFilter(final String applicationName) {
		this.applicationName = applicationName;
		this.issuer = "KarAuth";
	}

	public AuthenticationFilter(final String applicationName, final String issuer) {
		this.applicationName = applicationName;
		this.issuer = issuer;
	}

	public String getRequestedPath(final ContainerRequestContext requestContext) {
		final Class<?> resourceClass = this.resourceInfo.getResourceClass();
		final Method resourceMethod = this.resourceInfo.getResourceMethod();
		final String classPath = resourceClass.isAnnotationPresent(Path.class)
				? resourceClass.getAnnotation(Path.class).value()
				: "";
		final String methodPath = resourceMethod.isAnnotationPresent(Path.class)
				? resourceMethod.getAnnotation(Path.class).value()
				: "";
		final String fullPath = (classPath.startsWith("/") ? "" : "/") + classPath
				+ (methodPath.startsWith("/") ? "" : "/") + methodPath;
		return fullPath;
	}

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {
		/* logger.debug("-----------------------------------------------------"); logger.debug("----          Check if have authorization        ----");
		 * logger.debug("-----------------------------------------------------"); logger.debug("   for:{}", requestContext.getUriInfo().getPath()); */

		final Method method = this.resourceInfo.getResourceMethod();
		// Access denied for all
		if (method.isAnnotationPresent(DenyAll.class)) {
			LOGGER.debug("   ==> deny all {}", requestContext.getUriInfo().getPath());
			abortWithForbidden(requestContext, "Access blocked !!!");
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
			LOGGER.error("   ==> missing @RolesAllowed {}", requestContext.getUriInfo().getPath());
			abortWithForbidden(requestContext, "Access ILLEGAL !!!");
			return;
		}

		// Get the Authorization header from the request
		String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
		String apikeyHeader = requestContext.getHeaderString(APIKEY);
		// logger.debug("authorizationHeader: {}", authorizationHeader);
		if (authorizationHeader == null && apikeyHeader == null && method.isAnnotationPresent(PermitTokenInURI.class)) {
			final MultivaluedMap<String, String> quaryparam = requestContext.getUriInfo().getQueryParameters();
			for (final Entry<String, List<String>> item : quaryparam.entrySet()) {
				if ((authorizationHeader == null && HttpHeaders.AUTHORIZATION.equals(item.getKey()))
						&& !item.getValue().isEmpty()) {
					authorizationHeader = item.getValue().get(0);
				}
				if ((apikeyHeader == null && APIKEY.equals(item.getKey())) && !item.getValue().isEmpty()) {
					apikeyHeader = item.getValue().get(0);
				}
			}
		}
		// logger.debug("authorizationHeader: {}", authorizationHeader);
		final boolean isApplicationToken = apikeyHeader != null;
		final boolean isJwtToken = isTokenBasedAuthentication(authorizationHeader);
		if (!isApplicationToken && !isJwtToken) {
			LOGGER.warn("REJECTED unauthorized: {}", requestContext.getUriInfo().getPath());
			abortWithUnauthorized(requestContext, "REJECTED unauthorized: " + requestContext.getUriInfo().getPath());
			return;
		}
		UserByToken userByToken = null;
		if (isJwtToken) {
			// Extract the token from the Authorization header (Remove "Bearer ")
			final String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();
			// logger.debug("token: {}", token);
			try {
				userByToken = validateJwtToken(token);
			} catch (final Exception e) {
				LOGGER.error("Fail to validate token: {}", e.getMessage());
				abortWithUnauthorized(requestContext, "Fail to validate token: " + e.getMessage());
				return;
			}
			if (userByToken == null) {
				LOGGER.warn("get a NULL user ...");
				abortWithUnauthorized(requestContext, "get a NULL user ...");
				return;
			}
		} else {
			final String token = apikeyHeader.trim();
			try {
				userByToken = validateToken(token);
			} catch (final Exception e) {
				LOGGER.error("Fail to validate token: {}", e.getMessage());
				abortWithUnauthorized(requestContext, "Fail to validate token: " + e.getMessage());
				return;
			}
			if (userByToken == null) {
				LOGGER.warn("get a NULL application ...");
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
		try {
			haveRight = checkRight(requestContext, userContext, roles);
		} catch (final SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Is user valid?
		if (!haveRight) {
			LOGGER.error("REJECTED not enought right : {} require: {}", requestContext.getUriInfo().getPath(), roles);
			requestContext
					.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Not enought RIGHT !!!").build());
			return;
		}
		requestContext.setSecurityContext(userContext);
		// logger.debug("Get local user : {} / {}", user, userByToken);
	}

	protected boolean checkRight(
			final ContainerRequestContext requestContext,
			final MySecurityContext userContext,
			final List<String> roles) throws SystemException {
		for (final String role : roles) {
			if (userContext.isUserInRole(this.applicationName + "/" + role)) {
				return true;
			}
		}
		return false;
	}

	private boolean isTokenBasedAuthentication(final String authorizationHeader) {
		// Check if the Authorization header is valid
		// It must not be null and must be prefixed with "Bearer" plus a whitespace
		// The authentication scheme comparison must be case-insensitive
		return authorizationHeader != null
				&& authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
	}

	private void abortWithUnauthorized(final ContainerRequestContext requestContext, final String message) {

		// Abort the filter chain with a 401 status code response
		// The WWW-Authenticate header is sent along with the response
		LOGGER.warn("abortWithUnauthorized:");
		final RestErrorResponse ret = new RestErrorResponse(Response.Status.UNAUTHORIZED, "Unauthorized", message);
		LOGGER.error("Error UUID={}", ret.uuid);
		requestContext.abortWith(Response.status(ret.status)
				.header(HttpHeaders.WWW_AUTHENTICATE,
						AUTHENTICATION_SCHEME + " base64(HEADER).base64(CONTENT).base64(KEY)")
				.entity(ret).type(MediaType.APPLICATION_JSON).build());
	}

	private void abortWithForbidden(final ContainerRequestContext requestContext, final String message) {
		final RestErrorResponse ret = new RestErrorResponse(Response.Status.FORBIDDEN, "FORBIDDEN", message);
		LOGGER.error("Error UUID={}", ret.uuid);
		requestContext.abortWith(Response.status(ret.status).header(HttpHeaders.WWW_AUTHENTICATE, message).entity(ret)
				.type(MediaType.APPLICATION_JSON).build());
	}

	protected UserByToken validateToken(final String authorization) throws Exception {
		LOGGER.info("Must be Override by the application implmentation, otherwise it dose not work");
		return null;
	}

	// must be override to be good implementation
	protected UserByToken validateJwtToken(final String authorization) throws Exception {
		// logger.debug(" validate token : " + authorization);
		final JWTClaimsSet ret = JWTWrapper.validateToken(authorization, this.issuer, null);
		// check the token is valid !!! (signed and coherent issuer...
		if (ret == null) {
			LOGGER.error("The token is not valid: '{}'", authorization);
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
			LOGGER.info("Detect right in Authentication Filter: {}", rowRight);
			user.right = (Map<String, Map<String, Object>>) ret.getClaim("right");
			/*
			if (rights.containsKey(this.applicationName)) {
				user.right = rights.get(this.applicationName);
			} else {
				LOGGER.error("Connect with no right for this application='{}' full Right='{}'", this.applicationName,
						rights);
			}
			*/
		}
		// logger.debug("request user: '{}' right: '{}' row='{}'", userUID, user.right, rowRight);
		return user;
		// return UserDB.getUserOrCreate(id, (String)ret.getClaim("login") );
	}
}
