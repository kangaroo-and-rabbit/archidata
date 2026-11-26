/**
 * Exceptions of archidata.
 *
 * <p>The ones that carry an HTTP status are converted to REST error responses with that status by the
 * {@link org.atriasoft.archidata.catcher} exception mappers. The others (for instance
 * {@code DataAccessException}, {@code ProtectedFieldEncodingException} and {@code RESTErrorResponseException})
 * have no status of their own: they are answered by the generic mapper as a 500. See
 * {@code doc/error_handling.md}.</p>
 */
package org.atriasoft.archidata.exception;
