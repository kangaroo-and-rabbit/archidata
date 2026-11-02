package org.atriasoft.archidata.api;

import java.io.IOException;

import org.atriasoft.archidata.annotation.apiGenerator.ApiAsyncType;
import org.atriasoft.archidata.backup.BackupService;
import org.atriasoft.archidata.backup.BackupStart;
import org.atriasoft.archidata.backup.BackupState;
import org.atriasoft.archidata.catcher.RestErrorResponse;
import org.atriasoft.archidata.exception.FailException;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
 * application.
 * </p>
 */
@Path("/backups")
@Produces(MediaType.APPLICATION_JSON)
public class BackupResource {
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

	private static BackupService requireService() throws FailException {
		final BackupService current = service;
		if (current == null) {
			throw new FailException(Response.Status.SERVICE_UNAVAILABLE, "Backups are not configured");
		}
		return current;
	}

	/**
	 * Tell whether a backup is running, how the last attempt ended, and which archives exist.
	 * @return the state of the backups
	 * @throws FailException 503 when the application configured no backup service
	 * @throws IOException if the folder can not be read; the generic catcher answers 500 without its message
	 */
	@GET
	@RolesAllowed("ADMIN")
	@Operation(description = "Get the state of the backups and the list of the archives")
	public BackupState getState() throws FailException, IOException {
		return requireService().getState();
	}

	/**
	 * Start a backup and answer without waiting for it: read {@link #getState()} to follow it.
	 * @return 202 when the backup started, 507 when the volume has too little free space
	 * @throws FailException 409 when a backup is already running, 503 when the application configured no backup service
	 */
	@POST
	@RolesAllowed("ADMIN")
	@ApiAsyncType(Void.class)
	@Operation(description = "Start a backup: 202 when it started, 409 when one is already running, 507 when the disk has too little free space")
	public Response start() throws FailException {
		final BackupStart result = requireService().startAsync();
		if (result == BackupStart.ALREADY_RUNNING) {
			throw new FailException(Response.Status.CONFLICT, "A backup is already running");
		}
		if (result == BackupStart.INSUFFICIENT_SPACE) {
			return insufficientStorage();
		}
		return Response.accepted().build();
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
