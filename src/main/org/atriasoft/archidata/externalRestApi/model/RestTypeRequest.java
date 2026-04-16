package org.atriasoft.archidata.externalRestApi.model;

/**
 * Enumerates the supported HTTP method types for REST API endpoints.
 *
 * <p>Includes standard HTTP methods (GET, POST, PUT, PATCH, DELETE) and
 * custom methods (RESTORE, ARCHIVE, CALL).
 */
public enum RestTypeRequest {
	/** HTTP GET method. */
	GET,
	/** HTTP POST method. */
	POST,
	/** HTTP PUT method. */
	PUT,
	/** HTTP PATCH method. */
	PATCH,
	/** HTTP DELETE method. */
	DELETE,
	/** Custom RESTORE method for un-archiving resources. */
	RESTORE,
	/** Custom ARCHIVE method for soft-deleting resources. */
	ARCHIVE,
	/** Custom CALL method for RPC-style endpoints. */
	CALL
}
