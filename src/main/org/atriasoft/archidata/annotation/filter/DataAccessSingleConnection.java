package org.atriasoft.archidata.annotation.filter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.ws.rs.NameBinding;

/**
 * Annotation to activate a per-request single DB connection context on a Jakarta REST endpoint or resource class.
 *
 * <p>When applied to a JAX-RS resource class or method, this annotation triggers the opening of a single
 * {@code DBAccess} connection stored in a {@code ThreadLocal} at the beginning of the request,
 * and ensures it is properly closed at the end of the request.</p>
 *
 * <p>This allows all calls to {@code DataAccessConnectionContext.getConnection()} within the request
 * to reuse the same database connection without manually managing connection lifecycle.</p>
 *
 * <p><b>Important:</b> For this annotation to have effect, the corresponding JAX-RS filter
 * (e.g., {@code DataAccessRetentionConnectionFilter}) must be registered in your application and annotated with
 * {@code @DataAccessSingleConnection} to bind the filter to this annotation.</p>
 *
 * <h3>Example usage:</h3>
 * <pre>
 * &#64;Path("/users")
 * public class UserResource {
 *
 *     &#64;GET
 *     &#64;Path("/{id}")
 *     &#64;DataAccessSingleConnection
 *     public User getUser(@PathParam("id") String id) {
 *         DBAccess db = DataAccessConnectionContext.getConnection();
 *         return db.queryUserById(id);
 *     }
 * }
 * </pre>
 *
 * <p>Only requests to endpoints annotated with {@code @DataAccessSingleConnection} will have the
 * connection context automatically opened and closed.</p>
 */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface DataAccessSingleConnection {

}
