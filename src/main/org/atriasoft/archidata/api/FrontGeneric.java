package org.atriasoft.archidata.api;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

/**
 * JAX-RS resource that serves static front-end files (HTML, JS, CSS, images, etc.) from a configurable folder.
 *
 * <p>Subclass this resource and set {@link #baseFrontFolder} to point to your front-end build output directory.</p>
 */
public class FrontGeneric {
	private static final Logger LOGGER = LoggerFactory.getLogger(FrontGeneric.class);

	/** Base directory path from which front-end static files are served. */
	protected String baseFrontFolder = "/data/front";

	private String getExtension(final String filename) {
		if (filename.contains(".")) {
			return filename.substring(filename.lastIndexOf(".") + 1);
		}
		return "";
	}

	private Response retrive(final String fileName) throws Exception {
		String filePathName = this.baseFrontFolder + File.separator + fileName;
		final String extention = getExtension(filePathName);
		String mineType = null;
		LOGGER.debug("try retrive : '{}' '{}'", filePathName, extention);
		if (extention.length() != 0 && extention.length() <= 5) {
			if (extention.equalsIgnoreCase("jpg") || extention.equalsIgnoreCase("jpeg")) {
				mineType = "image/jpeg";
			} else if (extention.equalsIgnoreCase("gif")) {
				mineType = "image/gif";
			} else if (extention.equalsIgnoreCase("png")) {
				mineType = "image/png";
			} else if (extention.equalsIgnoreCase("svg")) {
				mineType = "image/svg+xml";
			} else if (extention.equalsIgnoreCase("webp")) {
				mineType = "image/webp";
			} else if (extention.equalsIgnoreCase("js")) {
				mineType = "application/javascript";
			} else if (extention.equalsIgnoreCase("json")) {
				mineType = "application/json";
			} else if (extention.equalsIgnoreCase("ico")) {
				mineType = "image/x-icon";
			} else if (extention.equalsIgnoreCase("html")) {
				mineType = "text/html";
			} else if (extention.equalsIgnoreCase("css")) {
				mineType = "text/css";
			} else if (extention.equalsIgnoreCase("mka")) {
				mineType = "audio/x-matroska";
			} else if (extention.equalsIgnoreCase("mkv")) {
				mineType = "video/x-matroska";
			} else if (extention.equalsIgnoreCase("webm")) {
				mineType = "video/webm";
			} else {
				throw new NotSupportedException("Not supported model: '" + fileName + "'");
			}
		} else {
			mineType = "text/html";
			filePathName = this.baseFrontFolder + File.separator + "index.html";
		}
		LOGGER.debug("    ==> '[}'", filePathName);
		// reads input image
		final File download = new File(filePathName);
		if (!download.exists()) {
			throw new NotFoundException("Not Found: '" + fileName + "' extension='" + extention + "'");
		}
		final ResponseBuilder response = Response.ok(download);
		// use this if I want to download the file:
		// response.header("Content-Disposition", "attachment; filename=" + fileName);
		final CacheControl cc = new CacheControl();
		cc.setMaxAge(60);
		cc.setNoCache(false);
		response.cacheControl(cc);
		response.type(mineType);

		return response.build();
	}

	/**
	 * Serves the index.html page for root GET requests.
	 * @return A response containing the index.html file.
	 * @throws Exception If the file cannot be read or the MIME type is unsupported.
	 */
	@GET
	@PermitAll()
	@Operation(description = "Retrieve native element (index)", tags = "SYSTEM")
	// @Produces(MediaType.APPLICATION_OCTET_STREAM)
	// @CacheMaxAge(time = 1, unit = TimeUnit.DAYS)
	public Response retrive0() throws Exception {
		return retrive("index.html");
	}

	/**
	 * Serves a specific file from the front-end directory based on the URL path segments.
	 * @param segments The URL path segments identifying the file to serve.
	 * @return A response containing the requested file.
	 * @throws Exception If the file is not found or the MIME type is unsupported.
	 */
	@GET
	@Path("{any: .*}")
	@PermitAll()
	@Operation(description = "Get specific file from the front environment", tags = "SYSTEM")
	// @Produces(MediaType.APPLICATION_OCTET_STREAM)
	// @CacheMaxAge(time = 10, unit = TimeUnit.DAYS)
	public Response retrive1(@PathParam("any") final List<PathSegment> segments) throws Exception {
		String filename = "";
		for (final PathSegment elem : segments) {
			if (!filename.isEmpty()) {
				filename += File.separator;
			}
			filename += elem.getPath();
		}
		return retrive(filename);
	}
}
