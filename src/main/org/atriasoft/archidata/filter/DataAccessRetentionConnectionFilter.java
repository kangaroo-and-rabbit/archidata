package org.atriasoft.archidata.filter;

import java.io.IOException;

import org.atriasoft.archidata.annotation.filter.DataAccessSingleConnection;
import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.exception.DataAccessException;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@DataAccessSingleConnection
public class DataAccessRetentionConnectionFilter implements ContainerRequestFilter, ContainerResponseFilter {

	private static final ThreadLocal<DataAccessConnectionContext> contextHolder = new ThreadLocal<>();

	public void lock() throws IOException {
		try {
			contextHolder.set(new DataAccessConnectionContext());
		} catch (InternalServerErrorException | IOException | DataAccessException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
			throw new IOException("Fail to Accs to the DB: " + ex.getMessage());
		}

	}

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