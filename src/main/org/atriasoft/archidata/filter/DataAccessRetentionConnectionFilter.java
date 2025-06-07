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

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {
		try {
			contextHolder.set(new DataAccessConnectionContext());
		} catch (InternalServerErrorException | IOException | DataAccessException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
			throw new IOException("Fail to Accs to the DB: " + ex.getMessage());
		}
	}

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException {
		DataAccessConnectionContext ctx = contextHolder.get();
		if (ctx != null) {
			try {
				ctx.close();
			} finally {
				contextHolder.remove();
			}
		}
	}
}