package org.atriasoft.archidata.filter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
// https://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey
// https://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey/45814178#45814178
// https://stackoverflow.com/questions/32817210/how-to-access-jersey-resource-secured-by-rolesallowed

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.security.PermitTokenInURI;
import org.atriasoft.archidata.annotation.security.RightAllowed;
import org.atriasoft.archidata.catcher.RestErrorResponse;
import org.atriasoft.archidata.exception.SystemException;
import org.atriasoft.archidata.model.UserByToken;
import org.atriasoft.archidata.tools.JWTWrapper;
import org.bson.types.ObjectId;
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

/**
 * JAX-RS request filter that handles authentication and authorization.
 *
 * <p>
 * This filter intercepts incoming requests, validates JWT or API key tokens,
 * and checks role-based access permissions before allowing access to secured resources.
 * </p>
 */
//@PreMatching
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationFilter.class);
	@Context
	private ResourceInfo resourceInfo;
	/** The application name used as a prefix for role-based access control. */
	protected final String applicationName;
	/** The JWT issuer used for token validation. */
	protected final String issuer;

	/** The Bearer authentication scheme identifier. */
	public static final String AUTHENTICATION_SCHEME = "Bearer";
	/** The API key header name. */
	public static final String APIKEY = "ApiKey";

	/**
	 * Constructs an authentication filter with the given application name and default issuer "Karso".
	 *
	 * @param applicationName the application name used for role-based access control
	 */
	public AuthenticationFilter(final String applicationName) {
		this(applicationName, "Karso");
	}

	/**
	 * Constructs an authentication filter with the given application name and issuer.
	 *
	 * @param applicationName the application name used for role-based access control
	 * @param issuer the JWT issuer identifier for token validation
	 */
	public AuthenticationFilter(final String applicationName, final String issuer) {
		this.applicationName = applicationName;
		this.issuer = issuer;
	}

	/**
	 * Retrieves the full resource path of the current request by combining class and method path annotations.
	 *
	 * @param requestContext the container request context
	 * @return the full resource path
	 */
	public String getRequestedPath(final ContainerRequestContext requestContext) {
		final Class<?> resourceClass = this.resourceInfo.getResourceClass();
		final Method resourceMethod = this.resourceInfo.getResourceMethod();
		final String classPath = resourceClass.isAnnotationPresent(Path.class)
				? resourceClass.getAnnotation(Path.class).value()
				: "";
		final String methodPath = resourceMethod.isAnnotationPresent(Path.class)
				? AnnotationTools.getAnnotationIncludingInterfaces(resourceMethod, Path.class).value()
				: "";
		final String fullPath = (classPath.startsWith("/") ? "" : "/") + classPath
				+ (methodPath.startsWith("/") ? "" : "/") + methodPath;
		return fullPath;
	}

	/**
	 * Filters incoming requests to enforce authentication and authorization.
	 *
	 * <p>
	 * Validates tokens (JWT or API key), checks role-based access, and sets the security context.
	 * </p>
	 */
	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {
		/* logger.debug("-----------------------------------------------------"); logger.debug("----          Check if have authorization        ----");
		 * logger.debug("-----------------------------------------------------"); logger.debug("   for:{}", requestContext.getUriInfo().getPath()); */

		final Method method = this.resourceInfo.getResourceMethod();
		// Access denied for all
		if (AnnotationTools.methodHasAnnotation(method, DenyAll.class)) {
			LOGGER.debug("   ==> deny all {}", requestContext.getUriInfo().getPath());
			abortWithForbidden(requestContext, "Access blocked !!!");
			return;
		}

		// Access allowed for all
		if (AnnotationTools.methodHasAnnotation(method, PermitAll.class)) {
			// logger.debug(" ==> permit all " + requestContext.getUriInfo().getPath());
			// no control ...
			return;
		}
		// this is a security guard, all the API must define their access level:
		final boolean hasRolesAllowed = AnnotationTools.methodHasAnnotation(method, RolesAllowed.class);
		final boolean hasRightAllowed = AnnotationTools.methodHasAnnotation(method, RightAllowed.class);
		if (!hasRolesAllowed && !hasRightAllowed) {
			LOGGER.error("   ==> missing @RolesAllowed or @RightAllowed {}", requestContext.getUriInfo().getPath());
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
		//LOGGER.error("authorizationHeader: {}", authorizationHeader);
		//LOGGER.error("apikeyHeader: {}", apikeyHeader);
		final boolean isApplicationToken = apikeyHeader != null;
		final boolean isJwtToken = isTokenBasedAuthenticationBearer(authorizationHeader);
		final boolean isApiToken = isTokenBasedAuthenticationApiKey(authorizationHeader);
		if (!isApplicationToken && !isJwtToken && !isApiToken) {
			LOGGER.warn("REJECTED unauthorized: /{}", requestContext.getUriInfo().getPath());
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
			String token = null;
			if (isApiToken) {
				token = authorizationHeader.substring(APIKEY.length()).trim();
			} else {
				token = apikeyHeader.trim();
			}
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
		// check roles if @RolesAllowed is present:
		boolean roleOk = true;
		if (hasRolesAllowed) {
			final RolesAllowed rolesAnnotation = AnnotationTools.getAnnotationIncludingInterfaces(method,
					RolesAllowed.class);
			final List<String> roles = Arrays.asList(rolesAnnotation.value());
			try {
				roleOk = checkRole(requestContext, userContext, roles);
			} catch (final SystemException e) {
				LOGGER.error("Failed to check roles: {}", e.getMessage(), e);
				roleOk = false;
			}
		}
		// check rights if @RightAllowed is present:
		boolean rightOk = true;
		if (hasRightAllowed) {
			final RightAllowed rightAnnotation = AnnotationTools.getAnnotationIncludingInterfaces(method,
					RightAllowed.class);
			rightOk = checkResourceRight(userContext, rightAnnotation.right(), rightAnnotation.access());
		}

		// Both must pass (AND logic):
		if (!roleOk || !rightOk) {
			LOGGER.error("REJECTED not enough rights : {}", requestContext.getUriInfo().getPath());
			requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
					.entity(new RestErrorResponse(Response.Status.FORBIDDEN, "FORBIDDEN", "Not enough RIGHT !!!"))
					.build());
			return;
		}
		requestContext.setSecurityContext(userContext);
		// logger.debug("Get local user : {} / {}", user, userByToken);
	}

	/**
	 * Checks whether the authenticated user has at least one of the required roles (OR logic).
	 *
	 * @param requestContext the container request context
	 * @param userContext the security context containing user information
	 * @param roles the list of allowed roles
	 * @return {@code true} if the user has at least one required role
	 * @throws SystemException if an error occurs during role verification
	 */
	protected boolean checkRole(
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

	/**
	 * Checks whether the authenticated user has the required access level for a fine-grained right.
	 *
	 * @param userContext the security context containing user information
	 * @param rightName the right name to check (e.g., "articles")
	 * @param requiredAccess the required access level
	 * @return {@code true} if the user has the required access level
	 */
	protected boolean checkResourceRight(
			final MySecurityContext userContext,
			final String rightName,
			final PartRight requiredAccess) {
		return userContext.hasResourceRight(this.applicationName, rightName, requiredAccess);
	}

	private boolean isTokenBasedAuthenticationBearer(final String authorizationHeader) {
		// Check if the Authorization header is valid
		// It must not be null and must be prefixed with "Bearer" plus a whitespace
		// The authentication scheme comparison must be case-insensitive
		return authorizationHeader != null
				&& authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
	}

	private boolean isTokenBasedAuthenticationApiKey(final String authorizationHeader) {
		// Check if the Authorization header is valid
		// It must not be null and must be prefixed with "ApiKey" plus a whitespace
		// The authentication scheme comparison must be case-insensitive
		return authorizationHeader != null && authorizationHeader.toLowerCase().startsWith(APIKEY.toLowerCase() + " ");
	}

	private void abortWithUnauthorized(final ContainerRequestContext requestContext, final String message) {

		// Abort the filter chain with a 401 status code response
		// The WWW-Authenticate header is sent along with the response
		LOGGER.warn("abortWithUnauthorized:");
		final RestErrorResponse ret = new RestErrorResponse(Response.Status.UNAUTHORIZED, "Unauthorized", message);
		LOGGER.error("Error OID={}", ret.oid);
		requestContext.abortWith(Response.status(ret.status)
				.header(HttpHeaders.WWW_AUTHENTICATE,
						AUTHENTICATION_SCHEME + " base64(HEADER).base64(CONTENT).base64(KEY)")
				.entity(ret).type(MediaType.APPLICATION_JSON).build());
	}

	private void abortWithForbidden(final ContainerRequestContext requestContext, final String message) {
		final RestErrorResponse ret = new RestErrorResponse(Response.Status.FORBIDDEN, "FORBIDDEN", message);
		LOGGER.error("Error OID={}", ret.oid);
		requestContext.abortWith(Response.status(ret.status).header(HttpHeaders.WWW_AUTHENTICATE, message).entity(ret)
				.type(MediaType.APPLICATION_JSON).build());
	}

	/**
	 * Validates an API key token and returns the associated user.
	 *
	 * <p>
	 * This method should be overridden by application implementations.
	 * </p>
	 *
	 * @param authorization the API key token string
	 * @return the user associated with the token, or {@code null} if invalid
	 * @throws Exception if token validation fails
	 */
	protected UserByToken validateToken(final String authorization) throws Exception {
		LOGGER.info("Must be Override by the application implmentation, otherwise it dose not work");
		return null;
	}

	/**
	 * Validates a JWT token and returns the associated user.
	 *
	 * <p>
	 * Can be overridden by subclasses to customize JWT validation logic.
	 * </p>
	 *
	 * @param authorization the JWT token string (without the "Bearer " prefix)
	 * @return the user extracted from the JWT claims, or {@code null} if invalid
	 * @throws Exception if token validation or parsing fails
	 */
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
		final ObjectId oid = new ObjectId(userUID);
		final UserByToken user = new UserByToken();
		user.setOid(oid);
		user.setName((String) ret.getClaim("login"));
		user.setType(UserByToken.TYPE_USER);
		final Object rowRoles = ret.getClaim("roles");
		if (rowRoles != null) {
			LOGGER.trace("Detect roles in Authentication Filter: {}", rowRoles);
			user.setRoles(RightSafeCaster.safeCastAndTransform(rowRoles));
		}
		final Object rowRight = ret.getClaim("right");
		if (rowRight != null) {
			LOGGER.trace("Detect right in Authentication Filter: {}", rowRight);
			user.setRight(RightSafeCaster.safeCastAndTransform(rowRight));
		}
		// logger.debug("request user: '{}' right: '{}' row='{}'", userUID, user.right, rowRight);
		return user;
		// return UserDB.getUserOrCreate(id, (String)ret.getClaim("login") );
	}
}
