/**
 * JAX-RS filters for authentication, CORS, and database connection management.
 *
 * <p>Request and response filters that handle cross-cutting concerns:
 * JWT token validation, CORS headers, and database connection lifecycle.</p>
 *
 * <ul>
 * <li>{@link org.atriasoft.archidata.filter.AuthenticationFilter}: validates the Bearer/ApiKey token
 * and the {@code @RolesAllowed} / {@code @RightAllowed} rules, then installs a
 * {@link org.atriasoft.archidata.filter.MySecurityContext}. Extend it to plug the application's own
 * token validation.</li>
 * <li>{@link org.atriasoft.archidata.filter.CORSFilter}: adds the CORS headers to every response;
 * {@link org.atriasoft.archidata.filter.OptionFilter} answers the {@code OPTIONS} preflights.</li>
 * <li>{@link org.atriasoft.archidata.filter.DataAccessRetentionConnectionFilter}: one database
 * connection (and optional transaction) per request for the resources annotated
 * {@link org.atriasoft.archidata.annotation.filter.DataAccessSingleConnection}.</li>
 * <li>{@link org.atriasoft.archidata.filter.PartRight} and
 * {@link org.atriasoft.archidata.filter.GenericContext}: right levels and security principal.</li>
 * </ul>
 *
 * <p>Example of registration on the application:</p>
 * {@snippet lang = java :
 * final ResourceConfig rc = new ResourceConfig();
 * rc.register(MyAuthFilter.class); // a subclass of AuthenticationFilter
 * rc.register(CORSFilter.class);
 * rc.register(OptionFilter.class);
 * rc.register(DataAccessRetentionConnectionFilter.class);
 * }
 *
 * <p>See <code>doc/security.md</code> in the repository
 * (<a href="https://github.com/kangaroo-and-rabbit/archidata/blob/develop/doc/security.md">online</a>) and <code>doc/connectionManagement.md</code>.</p>
 *
 * @see org.atriasoft.archidata.annotation.filter
 */
package org.atriasoft.archidata.filter;
