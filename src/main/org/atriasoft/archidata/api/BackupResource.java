package org.atriasoft.archidata.api;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.atriasoft.archidata.annotation.apiGenerator.ApiAsyncType;
import org.atriasoft.archidata.backup.BackupService;
import org.atriasoft.archidata.backup.BackupStart;
import org.atriasoft.archidata.backup.BackupState;
import org.atriasoft.archidata.catcher.RestErrorResponse;
import org.atriasoft.archidata.exception.FailException;
import org.atriasoft.archidata.filter.GenericContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * JAX-RS resource to see the backups of the application and to start one.
 *
 * <p>
 * An archive holds the whole database in clear, password hashes included: this resource must stay without any route
 * to download, restore or delete one, and without any parameter that names a file. What it returns carries no file
 * name and no path.
 * </p>
 *
 * <p>
 * Register it like any resource, and give it the service at startup with {@link #setService(BackupService)}. The
 * {@code @RolesAllowed} of the routes are only enforced by an {@code AuthenticationFilter} registered on the
 * application: each route also checks its role, so that an application without this filter is refused.
 * </p>
 *
 * <p>
 * Each manual start is logged with the identifier of the user and its result, a line the nightly task never writes.
 * </p>
 */
@Path("/backups")
@Produces(MediaType.APPLICATION_JSON)
public class BackupResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(BackupResource.class);
	/** Grants a right that can read: {@code READ} or {@code READ_WRITE}. */
	private static final String ROLE_READ = "ADMIN:r";
	/** Grants a right that can write: {@code WRITE} or {@code READ_WRITE}. The bare role would ask for {@code READ_WRITE}. */
	private static final String ROLE_WRITE = "ADMIN:w";
	private static final int STATUS_INSUFFICIENT_STORAGE = 507;
	/** Jersey builds the resource by itself, and the applications register it by class to generate their API. */
	private static volatile BackupService service = null;

	/** Default constructor used by the JAX-RS resource framework. */
	public BackupResource() {}

	/**
	 * Set the service the routes work on.
	 * @param value the backup service of the application
	 */
	public static void setService(final BackupService value) {
		service = value;
	}

	/**
	 * Check the role here too: {@code @RolesAllowed} does nothing without an {@code AuthenticationFilter}, and an
	 * application that forgets it must be refused, not open.
	 */
	private static void requireRole(final SecurityContext sc, final String role) throws FailException {
		// As AuthenticationFilter does: a role without its application is looked for in the ?system? group.
		final String checked = sc.getUserPrincipal() instanceof final GenericContext context
				&& context.getApplicationName() != null ? context.getApplicationName() + "/" + role : role;
		if (!sc.isUserInRole(checked)) {
			throw new FailException(Response.Status.FORBIDDEN, "Not enough RIGHT !!!");
		}
	}

	private static BackupService requireService() throws FailException {
		final BackupService current = service;
		if (current == null) {
			throw new FailException(Response.Status.SERVICE_UNAVAILABLE, "Backups are not configured");
		}
		return current;
	}

	/**
	 * Tell whether a backup is running, how the last attempt ended, and which archives exist.
	 * @param sc the security context of the request
	 * @return the state of the backups
	 * @throws FailException 403 without the role, 503 when the application configured no backup service
	 * @throws IOException if the folder can not be read; the generic catcher answers 500 without its message
	 */
	@GET
	@RolesAllowed(ROLE_READ)
	@Operation(description = "Get the state of the backups and the list of the archives")
	public BackupState getState(@Context final SecurityContext sc) throws FailException, IOException {
		requireRole(sc, ROLE_READ);
		return requireService().getState();
	}

	/**
	 * Start a backup and answer without waiting for it: read {@link #getState(SecurityContext)} to follow it.
	 * @param sc the security context of the request
	 * @return 202 when the backup started, 507 when the volume has too little free space
	 * @throws FailException 403 without the role, 409 when a backup is already running, 503 when the application
	 *             configured no backup service
	 */
	@POST
	@RolesAllowed(ROLE_WRITE)
	@ApiAsyncType(Void.class)
	@Operation(description = "Start a backup: 202 when it started, 409 when one is already running, 507 when the disk has too little free space")
	public Response start(@Context final SecurityContext sc) throws FailException {
		requireRole(sc, ROLE_WRITE);
		final BackupService backup = requireService();
		final Object user = sc.getUserPrincipal() instanceof final GenericContext context ? context.getUserID() : null;
		final BackupStart result;
		try {
			result = backup.startAsync();
		} catch (final UncheckedIOException ex) {
			LOGGER.warn("Manual backup by user {}: FAILED", user);
			throw ex;
		}
		if (result == BackupStart.STARTED) {
			LOGGER.info("Manual backup by user {}: {}", user, result);
			return Response.accepted().build();
		}
		LOGGER.warn("Manual backup by user {}: {}", user, result);
		if (result == BackupStart.ALREADY_RUNNING) {
			throw new FailException(Response.Status.CONFLICT, "A backup is already running");
		}
		return insufficientStorage();
	}

	/** {@link Response.Status} has no 507 and {@code FailException} needs one; a catcher would hide a 5xx message. */
	private static Response insufficientStorage() {
		final Response.StatusType status = Response.status(STATUS_INSUFFICIENT_STORAGE, "Insufficient Storage").build()
				.getStatusInfo();
		final RestErrorResponse error = new RestErrorResponse(status, "Request Fail",
				"Not enough free space for a backup");
		return Response.status(status).entity(error).type(MediaType.APPLICATION_JSON).build();
	}
}
