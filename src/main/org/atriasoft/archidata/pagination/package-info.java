/**
 * Offset-based pagination carried by HTTP headers and the RFC 5988 {@code Link} header.
 *
 * <p>The client asks for a page with the {@code X-Pagination-Offset} and
 * {@code X-Pagination-Limit} headers (or the query parameters of the same name); the server answers
 * with the plain list of items, the total in {@code X-Total-Count} and the navigation links
 * ({@code first}, {@code prev}, {@code next}, {@code last}) in {@code Link}.</p>
 *
 * <p>Entry points:</p>
 * <ul>
 * <li>{@link org.atriasoft.archidata.pagination.PaginationFeature}: the Jersey feature to register
 * once on the application; it wires the two classes below.</li>
 * <li>{@link org.atriasoft.archidata.pagination.PaginationContextValueProvider}: resolves the
 * {@link org.atriasoft.archidata.annotation.method.PaginationContext}-annotated parameters (of type
 * {@link org.atriasoft.archidata.dataAccess.model.PaginationContext}) from the request.</li>
 * <li>{@link org.atriasoft.archidata.pagination.PaginationResponseFilter}: turns a returned
 * {@link org.atriasoft.archidata.dataAccess.model.Pagination} into body plus headers.</li>
 * <li>{@link org.atriasoft.archidata.pagination.LinkHeaderBuilder}: builds the {@code Link} value.</li>
 * </ul>
 *
 * <p>Example of a paginated endpoint (the {@code Condition} used for the page and for the total must
 * be the same):</p>
 * {@snippet lang = java :
 * rc.register(PaginationFeature.class); // once, on the ResourceConfig
 * }
 * {@snippet lang = java :
 * @GET
 * @RolesAllowed("USER")
 * public Pagination<Foo> list(@PaginationContext final PaginationContext page) throws Exception {
 * 	final List<Foo> items = DataAccess.gets(Foo.class, page.toSkip(), page.toLimit());
 * 	return new Pagination<>(items, DataAccess.count(Foo.class), page.offset(), page.limit());
 * }
 * }
 *
 * <p>See <code>doc/pagination.md</code> in the repository
 * (<a href="https://github.com/kangaroo-and-rabbit/archidata/blob/develop/doc/pagination.md">online</a>).</p>
 */
package org.atriasoft.archidata.pagination;
