/**
 * Reusable JAX-RS resource classes for common REST operations.
 *
 * <p>Provides ready-made resources that an application registers on its JAX-RS
 * {@code ResourceConfig} instead of rewriting them:</p>
 * <ul>
 * <li>{@link org.atriasoft.archidata.api.DataResource}: upload and download of binary files
 * ({@code /data}), with range requests served by {@link org.atriasoft.archidata.api.MediaStreamer}.</li>
 * <li>{@link org.atriasoft.archidata.api.openApiResource}: serves the generated OpenAPI 3.0.3
 * specification ({@code /openapi}); {@link org.atriasoft.archidata.api.SwaggerUiExternal} extends it
 * with an external Swagger UI.</li>
 * <li>{@link org.atriasoft.archidata.api.FrontGeneric}: serves the static files of a front-end build.</li>
 * <li>{@link org.atriasoft.archidata.api.ProxyResource}: GET proxy to external URLs, protected against
 * SSRF.</li>
 * </ul>
 *
 * <p>Example:</p>
 * {@snippet lang = java :
 * final ResourceConfig rc = new ResourceConfig();
 * rc.register(DataResource.class);
 * rc.register(openApiResource.class);
 * }
 *
 * <p>See <code>doc/openapi_generation.md</code> in the repository
 * (<a href="https://github.com/kangaroo-and-rabbit/archidata/blob/develop/doc/openapi_generation.md">online</a>).</p>
 *
 * @see org.atriasoft.archidata.dataAccess
 */
package org.atriasoft.archidata.api;
