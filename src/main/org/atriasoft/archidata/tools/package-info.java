/**
 * Utility classes for configuration, security, REST clients, and data handling.
 *
 * <p>Includes configuration management ({@link org.atriasoft.archidata.tools.ConfigBaseVariable}),
 * JWT token operations ({@link org.atriasoft.archidata.tools.JWTWrapper}),
 * REST client ({@link org.atriasoft.archidata.tools.RESTApi}),
 * UUID generation ({@link org.atriasoft.archidata.tools.UuidUtils}),
 * and various data processing utilities.</p>
 *
 * <ul>
 * <li>{@code ConfigBaseVariable}: database, API and SSO settings, read from the environment and
 * overridable with setters until {@code lock()} is called.</li>
 * <li>{@code JWTWrapper}: key initialisation ({@code initLocalToken}, {@code initValidateToken}),
 * token creation ({@code generateJWToken}) and validation (used by
 * {@link org.atriasoft.archidata.filter.AuthenticationFilter}).</li>
 * <li>{@code RESTApi} / {@link org.atriasoft.archidata.tools.RESTApiRequest}: fluent HTTP client.</li>
 * <li>{@link org.atriasoft.archidata.tools.SsrfGuard}: validation of the URLs the server fetches.</li>
 * </ul>
 *
 * <p>Example:</p>
 * {@snippet lang = java :
 * ConfigBaseVariable.setBdDatabase("my_app");
 * ConfigBaseVariable.setDbPassword("secret");
 * JWTWrapper.initLocalToken("my-application-uuid");
 *
 * final RESTApi api = new RESTApi("http://localhost:9000/api/");
 * final User user = api.request("users").get().fetch(User.class);
 * }
 *
 * <p>See <code>doc/security.md</code> in the repository
 * (<a href="https://github.com/kangaroo-and-rabbit/archidata/blob/develop/doc/security.md">online</a>).</p>
 */
package org.atriasoft.archidata.tools;
