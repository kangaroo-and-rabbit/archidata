package org.kar.archidata.filter;

import java.lang.reflect.Method;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;


import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.kar.archidata.UserDB;
import org.kar.archidata.annotation.PermitTokenInURI;
import org.kar.archidata.model.User;
import org.kar.archidata.model.UserSmall;
import org.kar.archidata.util.JWTWrapper;

import com.nimbusds.jwt.JWTClaimsSet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
// https://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey
// https://stackoverflow.com/questions/26777083/best-practice-for-rest-token-based-authentication-with-jax-rs-and-jersey/45814178#45814178
// https://stackoverflow.com/questions/32817210/how-to-access-jersey-resource-secured-by-rolesallowed

//@PreMatching
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
	@Context
	private ResourceInfo resourceInfo;
    
    private static final String AUTHENTICATION_SCHEME = "Yota";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        /*
        System.out.println("-----------------------------------------------------");
        System.out.println("----          Check if have authorization        ----");
        System.out.println("-----------------------------------------------------");
        System.out.println("   for:" + requestContext.getUriInfo().getPath());
        */
        Method method = resourceInfo.getResourceMethod();
        // Access denied for all
        if(method.isAnnotationPresent(DenyAll.class)) {
        	System.out.println("   ==> deny all " + requestContext.getUriInfo().getPath());
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).entity("Access blocked !!!").build());
            return;
        }
        
        //Access allowed for all
        if( method.isAnnotationPresent(PermitAll.class)) {
        	System.out.println("   ==> permit all " + requestContext.getUriInfo().getPath());
        	// no control ...
        	return;
        }
        // this is a security guard, all the API must define their access level:
        if(!method.isAnnotationPresent(RolesAllowed.class)) {
        	System.out.println("   ==> missin @RolesAllowed " + requestContext.getUriInfo().getPath());
            requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).entity("Access ILLEGAL !!!").build());
            return;
        	
        }

        // Get the Authorization header from the request
        String authorizationHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        //System.out.println("authorizationHeader: " + authorizationHeader);
        if(authorizationHeader == null && method.isAnnotationPresent(PermitTokenInURI.class)) {
        	MultivaluedMap<String, String> quaryparam = requestContext.getUriInfo().getQueryParameters();
            for (Entry<String, List<String>> item: quaryparam.entrySet()) {
            	if (item.getKey().equals(HttpHeaders.AUTHORIZATION)) {
            		if (!item.getValue().isEmpty()) {
            			authorizationHeader = item.getValue().get(0);
            		}
            		break;
            	}
            }
        }
        //System.out.println("authorizationHeader: " + authorizationHeader);
        
        
        /*
        System.out.println("   -------------------------------");
        // this get the parameters inside the pre-parsed element in the request ex: @Path("thumbnail/{id}") generate a map with "id"
        MultivaluedMap<String, String> pathparam = requestContext.getUriInfo().getPathParameters(); 
        for (Entry<String, List<String>> item: pathparam.entrySet()) {
            System.out.println("  param: " + item.getKey() + " ==>" + item.getValue());
        }
        System.out.println("   -------------------------------");
        // need to add "@QueryParam("p") String token, " in the model
        //MultivaluedMap<String, String> quaryparam = requestContext.getUriInfo().getQueryParameters();
        for (Entry<String, List<String>> item: quaryparam.entrySet()) {
            System.out.println("  query: " + item.getKey() + " ==>" + item.getValue());
        }
        System.out.println("   -------------------------------");
        List<PathSegment> segments = requestContext.getUriInfo().getPathSegments();
        for (final PathSegment item: segments) {
            System.out.println("  query: " + item.getPath() + " ==>" + item.getMatrixParameters());
        }
        System.out.println("   -------------------------------");
        MultivaluedMap<String, String> headers = requestContext.getHeaders(); 
        for (Entry<String, List<String>> item: headers.entrySet()) {
            System.out.println("  headers: " + item.getKey() + " ==>" + item.getValue());
        }
        System.out.println("   -------------------------------");
		*/
        // Validate the Authorization header data Model "Yota userId:token"
        if (!isTokenBasedAuthentication(authorizationHeader)) {
        	System.out.println("REJECTED unauthorized: " + requestContext.getUriInfo().getPath());
            abortWithUnauthorized(requestContext);
            return;
        }
        // check JWT token (basic:)

        // Extract the token from the Authorization header (Remove "Yota ")
        String token = authorizationHeader.substring(AUTHENTICATION_SCHEME.length()).trim();
        System.out.println("token: " + token);
        
        
        User user = null;
        try {
            user = validateToken(token);
        } catch (Exception e) {
            abortWithUnauthorized(requestContext);
        }
        if (user == null) {
            abortWithUnauthorized(requestContext);
        }
        // create the security context model:
        String scheme = requestContext.getUriInfo().getRequestUri().getScheme();
        MySecurityContext userContext = new MySecurityContext(user, scheme);
        // retrieve the allowed right:
        RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
        List<String> roles = Arrays.asList(rolesAnnotation.value());
        // check if the user have the right:
        boolean haveRight = false;
        for (String role : roles) {
        	if (userContext.isUserInRole(role)) {
        		haveRight = true;
        		break;
        	}
        }
        
        //Is user valid?
        if( ! haveRight) {
        	System.out.println("REJECTED not enought right : " + requestContext.getUriInfo().getPath() + " require: " + roles);
        	requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Not enought RIGHT !!!").build());
            return;
        }
        requestContext.setSecurityContext(userContext);
        System.out.println("Get local user : " + user);
    }
    
    private boolean isTokenBasedAuthentication(String authorizationHeader) {
        // Check if the Authorization header is valid
        // It must not be null and must be prefixed with "Bearer" plus a whitespace
        // The authentication scheme comparison must be case-insensitive
        return authorizationHeader != null && authorizationHeader.toLowerCase().startsWith(AUTHENTICATION_SCHEME.toLowerCase() + " ");
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext) {

        // Abort the filter chain with a 401 status code response
        // The WWW-Authenticate header is sent along with the response
        requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                        .header(HttpHeaders.WWW_AUTHENTICATE,
                                AUTHENTICATION_SCHEME + " base64(HEADER).base64(CONTENT).base64(KEY)")
                        .build());
    }

    private User validateToken(String authorization) throws Exception {
        System.out.println(" validate token : " + authorization);
        JWTClaimsSet ret = JWTWrapper.validateToken(authorization, "KarAuth");
        // check the token is valid !!! (signed and coherent issuer...
        if (ret == null) {
            System.out.println("The token is not valid: '" + authorization + "'");
        	return null;
        }
        // check userID
        String userUID = ret.getSubject();
        long id = Long.parseLong(userUID);
        System.out.println("request user: '" + userUID + "'");
        return UserDB.getUserOrCreate(id, (String)ret.getClaim("login") );
    }
}
