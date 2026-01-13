package org.atriasoft.archidata.filter;

import java.io.IOException;

import org.atriasoft.archidata.annotation.filter.DataAccessSingleConnection;
import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS filter that manages database connection lifecycle per request.
 *
 * <p>
 * This filter opens a database connection at the start of the request and closes it
 * at the end, ensuring all database operations within the request share the same connection.
 * </p>
 *
 * <p>
 * <strong>Virtual Thread Support:</strong> This filter uses ThreadLocal for connection
 * storage. Each virtual thread has its own ThreadLocal storage (not shared with carrier
 * thread), so connections are safely isolated between virtual threads.
 * </p>
 */
@Provider
@DataAccessSingleConnection
public class DataAccessRetentionConnectionFilter implements ContainerRequestFilter, ContainerResponseFilter {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataAccessRetentionConnectionFilter.class);

	private static final ThreadLocal<DataAccessConnectionContext> contextHolder = new ThreadLocal<>();

	/**
	 * Opens a database connection for the current thread.
	 *
	 * @throws IOException if connection fails
	 */
	public void lock() throws IOException {
		try {
			contextHolder.set(new DataAccessConnectionContext());
		} catch (final InternalServerErrorException | IOException | DataAccessException ex) {
			LOGGER.error("Failed to open database connection: {}", ex.getMessage(), ex);
			throw new IOException("Failed to access the database: " + ex.getMessage(), ex);
		}
	}

	/**
	 * Closes the database connection for the current thread.
	 *
	 * @throws IOException if closing fails
	 */
	public void unlock() throws IOException {
		final DataAccessConnectionContext ctx = contextHolder.get();
		if (ctx != null) {
			try {
				ctx.close();
			} finally {
				contextHolder.remove();
			}
		}
	}

	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException {
		lock();
	}

	@Override
	public void filter(final ContainerRequestContext requestContext, final ContainerResponseContext responseContext)
			throws IOException {
		unlock();
	}
}
