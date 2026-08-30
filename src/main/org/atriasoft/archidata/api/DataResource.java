package org.atriasoft.archidata.api;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.atriasoft.archidata.annotation.apiGenerator.ApiInputOptional;
import org.atriasoft.archidata.annotation.apiGenerator.ApiTypeScriptProgress;
import org.atriasoft.archidata.annotation.filter.DataAccessSingleConnection;
import org.atriasoft.archidata.annotation.security.PermitTokenInURI;
import org.atriasoft.archidata.dataAccess.DataAccess;
import com.mongodb.client.model.Filters;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.exception.FailException;
import org.atriasoft.archidata.filter.GenericContext;
import org.atriasoft.archidata.model.Data;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.DataTools;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;

// https://stackoverflow.com/questions/35367113/jersey-webservice-scalable-approach-to-download-file-and-reply-to-client
// https://gist.github.com/aitoroses/4f7a2b197b732a6a691d

/**
 * JAX-RS resource for uploading, retrieving, and streaming binary data (files, images, audio, video).
 *
 * <p>Data is stored on disk with SHA-512 deduplication and registered in a MongoDB collection.</p>
 */
@Path("/data")
@Produces(MediaType.APPLICATION_JSON)
public class DataResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataResource.class);
	private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
	private static final int CHUNK_SIZE_IN = 50 * 1024 * 1024; // 1MB chunks
	/** How long the browser may keep a data without asking again.
	 *
	 * <p>
	 * The content behind an object id never changes: another content is another object. A year is
	 * what the specification allows at most, and "immutable" tells the browser not even to ask when
	 * the page is reloaded. It stays private: what is behind a token must not end up in a shared
	 * cache. */
	private static final String DATA_CACHE_CONTROL = "private, max-age=31536000, immutable";
	/** Counter for generating unique temporary file identifiers. */
	private static long tmpFolderId = 1;

	/** Default constructor used by the JAX-RS resource framework. */
	public DataResource() {}

	private static void createFolder(final String path) throws IOException {
		if (!Files.exists(java.nio.file.Path.of(path))) {
			// Log.print("Create folder: " + path);
			Files.createDirectories(java.nio.file.Path.of(path));
		}
	}

	/**
	 * Returns the next unique temporary data identifier.
	 * @return A unique long identifier for temporary file storage.
	 */
	public static long getTmpDataId() {
		return tmpFolderId++;
	}

	/**
	 * Returns the file path for a temporary data file, creating the temporary directory if needed.
	 * @param tmpFolderId The unique identifier for the temporary file.
	 * @return The absolute path to the temporary file.
	 */
	public static String getTmpFileInData(final long tmpFolderId) {
		final String filePath = ConfigBaseVariable.getTmpDataFolder() + File.separator + tmpFolderId;
		try {
			createFolder(ConfigBaseVariable.getTmpDataFolder() + File.separator);
		} catch (final IOException e) {
			LOGGER.error("Failed to create tmp data folder: {}", e.getMessage(), e);
		}
		return filePath;
	}

	/**
	 * Returns the file path for data stored under the old UUID-based layout.
	 * @param uuid The UUID identifying the data.
	 * @return The absolute path to the data file.
	 */
	public static String getFileDataOld(final UUID uuid) {
		final String stringUUID = uuid.toString();
		final String part1 = stringUUID.substring(0, 2);
		final String part2 = stringUUID.substring(2, 4);
		final String part3 = stringUUID.substring(4);
		final String finalPath = part1 + File.separator + part2;
		String filePath = ConfigBaseVariable.getMediaDataFolder() + "_uuid" + File.separator + finalPath
				+ File.separator;
		try {
			createFolder(filePath);
		} catch (final IOException e) {
			LOGGER.error("Failed to create data folder for UUID: {}", e.getMessage(), e);
		}
		filePath += part3;
		return filePath;
	}

	/**
	 * Returns the file path for data stored under the ObjectId-based layout, using SHA-256 hashing for directory distribution.
	 * @param oid The ObjectId identifying the data.
	 * @return The absolute path to the data file.
	 */
	public static String getFileData(final ObjectId oid) {
		final String stringOid = oid.toHexString();
		String dir1 = stringOid.substring(0, 2);
		String dir2 = stringOid.substring(2, 4);
		String dir3 = stringOid.substring(4, 6);
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final byte[] hashBytes = digest.digest(oid.toByteArray());
			dir1 = String.format("%02x", hashBytes[0]);
			dir2 = String.format("%02x", hashBytes[1]);
			dir3 = String.format("%02x", hashBytes[2]);
		} catch (final NoSuchAlgorithmException ex) {
			LOGGER.error("Fail to generate the hash of the objectId ==> ise direct value ... {}", ex.getMessage());
		}
		final String finalPath = dir1 + File.separator + dir2 + File.separator + dir3;
		String filePath = ConfigBaseVariable.getMediaDataFolder() + File.separator + finalPath + File.separator;
		try {
			createFolder(filePath);
		} catch (final IOException e) {
			LOGGER.error("Failed to create data folder for OID: {}", e.getMessage(), e);
		}
		filePath += stringOid;
		return filePath;
	}

	/**
	 * Returns the file path for the JSON metadata associated with the given ObjectId.
	 * @param oid The ObjectId identifying the data.
	 * @return The absolute path to the metadata JSON file.
	 */
	public static String getFileMetaData(final ObjectId oid) {
		return getFileData(oid) + ".json";
	}

	/**
	 * Retrieves a {@link Data} record matching the given SHA-512 hash.
	 * @param sha512 The SHA-512 hash to search for.
	 * @return The matching {@link Data} record, or {@code null} if not found.
	 */
	public Data getWithSha512(final String sha512) {
		LOGGER.info("find sha512 = {}", sha512);
		try {
			return DataAccess.get(Data.class, new Condition(Filters.eq("sha512", sha512)));
		} catch (final Exception e) {
			LOGGER.error("Failed to get data with sha512: {}", e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Retrieves a {@link Data} record by its numeric identifier.
	 * @param id The numeric identifier to search for.
	 * @return The matching {@link Data} record, or {@code null} if not found.
	 */
	public Data getWithId(final long id) {
		LOGGER.info("find id = {}", id);
		try {
			return DataAccess.getById(Data.class, id);
		} catch (final Exception e) {
			LOGGER.error("Failed to get data with id: {}", e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Resolves a file extension to its corresponding MIME type.
	 * @param extension The file extension (e.g. "jpg", "png").
	 * @return The MIME type string.
	 * @throws IOException If the extension is not recognized.
	 */
	protected String getMimeType(final String extension) throws IOException {
		return switch (extension.toLowerCase()) {
			case "jpg", "jpeg" -> "image/jpeg";
			case "png" -> "image/png";
			case "webp" -> "image/webp";
			case "mka" -> "audio/x-matroska";
			case "mkv" -> "video/x-matroska";
			case "webm" -> "video/webm";
			default -> throw new IOException("Can not find the mime type of data input: '" + extension + "'");
		};
	}

	/**
	 * Creates a new {@link Data} record in the database and moves the temporary file to permanent storage.
	 * @param tmpUID The temporary file identifier.
	 * @param originalFileName The original file name (used to determine MIME type).
	 * @param sha512 The SHA-512 hash of the file content.
	 * @return The newly created {@link Data} record, or {@code null} on insertion failure.
	 * @throws IOException If the MIME type cannot be determined or the file cannot be moved.
	 */
	public Data createNewData(final long tmpUID, final String originalFileName, final String sha512)
			throws IOException {
		// determine mime type:
		Data injectedData = new Data();
		String mimeType = "";
		final String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1);
		mimeType = getMimeType(extension);
		injectedData.setMimeType(mimeType);
		injectedData.setSha512(sha512);
		final String tmpPath = getTmpFileInData(tmpUID);
		injectedData.setSize(Files.size(Paths.get(tmpPath)));

		try {
			injectedData = DataAccess.insert(injectedData);
		} catch (final Exception e) {
			LOGGER.error("Failed to insert data: {}", e.getMessage(), e);
			return null;
		}
		final String mediaPath = getFileData(injectedData.getOid());
		LOGGER.info("src = {}", tmpPath);
		LOGGER.info("dst = {}", mediaPath);
		try {
			Files.move(Paths.get(tmpPath), Paths.get(mediaPath), StandardCopyOption.ATOMIC_MOVE);
		} catch (final AtomicMoveNotSupportedException ex) {
			Files.move(Paths.get(tmpPath), Paths.get(mediaPath), StandardCopyOption.REPLACE_EXISTING);
		}
		LOGGER.info("Move done");
		return injectedData;
	}

	/**
	 * Moves a data file from the old UUID-based storage layout to the new ObjectId-based layout, including metadata.
	 * @param uuid The UUID of the data in the old layout.
	 * @param oid The ObjectId of the data in the new layout.
	 * @throws IOException If the file move fails.
	 */
	public static void modeFileOldModelToNewModel(final UUID uuid, final ObjectId oid) throws IOException {
		String mediaCurentPath = getFileDataOld(uuid);
		String mediaDestPath = getFileData(oid);
		LOGGER.info("src = {}", mediaCurentPath);
		LOGGER.info("dst = {}", mediaDestPath);
		if (Files.exists(Paths.get(mediaCurentPath))) {
			LOGGER.info("move: {} ==> {}", mediaCurentPath, mediaDestPath);
			try {
				Files.move(Paths.get(mediaCurentPath), Paths.get(mediaDestPath), StandardCopyOption.ATOMIC_MOVE);
				LOGGER.info("Atomic-move done");
			} catch (final AtomicMoveNotSupportedException ex) {
				Files.move(Paths.get(mediaCurentPath), Paths.get(mediaDestPath), StandardCopyOption.REPLACE_EXISTING);
				LOGGER.info("Move done");
			}
		}
		// Move old meta-data...
		mediaCurentPath = mediaCurentPath.substring(0, mediaCurentPath.length() - 4) + "meta.json";
		mediaDestPath = mediaDestPath.substring(0, mediaDestPath.length() - 4) + "meta.json";
		if (Files.exists(Paths.get(mediaCurentPath))) {
			LOGGER.info("moveM: {} ==> {}", mediaCurentPath, mediaDestPath);
			try {
				Files.move(Paths.get(mediaCurentPath), Paths.get(mediaDestPath), StandardCopyOption.ATOMIC_MOVE);
				LOGGER.info("Atomic-move done");
			} catch (final AtomicMoveNotSupportedException ex) {
				Files.move(Paths.get(mediaCurentPath), Paths.get(mediaDestPath), StandardCopyOption.REPLACE_EXISTING);
				LOGGER.info("Move done");
			}
		}
		LOGGER.info("Move done");
	}

	/**
	 * Saves an input stream to a temporary file and returns its SHA-512 hash.
	 * @param uploadedInputStream The input stream to save.
	 * @param idData The temporary file identifier.
	 * @return The SHA-512 hex string of the saved data.
	 * @throws FailException If the file cannot be written.
	 */
	public static String saveTemporaryFile(final InputStream uploadedInputStream, final long idData)
			throws FailException {
		return saveFile(uploadedInputStream, DataResource.getTmpFileInData(idData));
	}

	/**
	 * Deletes a temporary file if it exists.
	 * @param idData The temporary file identifier.
	 */
	public static void removeTemporaryFile(final long idData) {
		final String filepath = DataResource.getTmpFileInData(idData);
		if (Files.exists(Paths.get(filepath))) {
			try {
				Files.delete(Paths.get(filepath));
			} catch (final IOException e) {
				LOGGER.error("Can not delete temporary file '{}': {}", Paths.get(filepath), e.getMessage(), e);
			}
		}
	}

	// save uploaded file to a defined location on the server
	static String saveFile(final InputStream uploadedInputStream, final String serverLocation) throws FailException {
		String out = "";
		MessageDigest md = null;
		try (OutputStream outpuStream = new FileOutputStream(new File(serverLocation))) {
			md = MessageDigest.getInstance("SHA-512");
		} catch (final IOException ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Can not write in temporary file", ex);
		} catch (final NoSuchAlgorithmException ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Can not find sha512 algorithms", ex);
		}
		if (md != null) {
			try (OutputStream outpuStream = new FileOutputStream(new File(serverLocation))) {
				int read = 0;
				final byte[] bytes = new byte[CHUNK_SIZE_IN];
				while ((read = uploadedInputStream.read(bytes)) != -1) {
					// logger.info("write {}", read);
					md.update(bytes, 0, read);
					outpuStream.write(bytes, 0, read);
				}
				LOGGER.info("Flush input stream ... {}", serverLocation);
				outpuStream.flush();
				// create the end of sha512
				final byte[] sha512Digest = md.digest();
				// convert in hexadecimal
				out = bytesToHex(sha512Digest);
				uploadedInputStream.close();
			} catch (final IOException ex) {
				throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Can not write in temporary file", ex);
			}
		}
		return out;
	}

	/**
	 * Converts a byte array to its lowercase hexadecimal string representation.
	 * @param bytes The byte array to convert.
	 * @return The hexadecimal string.
	 */
	public static String bytesToHex(final byte[] bytes) {
		final StringBuilder sb = new StringBuilder();
		for (final byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	/**
	 * Retrieves a {@link Data} record by its ObjectId.
	 * @param oid The ObjectId to look up.
	 * @return The matching {@link Data} record, or {@code null} if not found.
	 */
	public Data getSmall(final ObjectId oid) {
		try {
			return DataAccess.getById(Data.class, oid);
		} catch (final Exception e) {
			LOGGER.error("Failed to get data by OID: {}", e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Uploads a file via multipart form data and stores it in the system.
	 * @param fileInputStream The uploaded file input stream.
	 * @param fileMetaData The multipart form metadata (file name, etc.).
	 * @return The ObjectId of the stored data record.
	 * @throws Exception If the upload or database insertion fails.
	 */
	@POST
	@Path("upload")
	@RolesAllowed({ "USER" })
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Operation(description = "Upload data in the system", tags = "SYSTEM")
	@ApiTypeScriptProgress
	@DataAccessSingleConnection
	public ObjectId uploadMedia(
			@FormDataParam("file") final InputStream fileInputStream,
			@FormDataParam("file") final FormDataContentDisposition fileMetaData) throws Exception {
		return DataTools.uploadData(fileInputStream, fileMetaData);
	}

	/**
	 * Downloads data from an external URI and stores it in the system.
	 * @param uri The external URI to download from.
	 * @return The ObjectId of the stored data record.
	 * @throws Exception If the download or database insertion fails.
	 */
	@POST
	@Path("uploadUri")
	@RolesAllowed({ "USER" })
	@Consumes({ MediaType.APPLICATION_JSON })
	@Operation(description = "Upload data in the system with an external URI", tags = "SYSTEM")
	@ApiTypeScriptProgress
	@DataAccessSingleConnection
	public ObjectId uploadMediaFromUri(@QueryParam("uri") @NotNull final String uri) throws Exception {
		return DataTools.uploadDataFromUri(uri);
	}

	/**
	 * Retrieves data by its ObjectId, supporting HTTP range requests for streaming.
	 * @param sc The security context.
	 * @param token Optional authorization token from query parameter.
	 * @param range The HTTP Range header value for partial content requests.
	 * @param oid The ObjectId of the data to retrieve.
	 * @return A streaming response with the data content.
	 * @throws FailException If the data is not found or the stream cannot be built.
	 */
	@GET
	@Path("{oid}")
	@PermitTokenInURI
	@RolesAllowed("USER")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(description = "Get back some data from the data environment", tags = "SYSTEM")
	public Response retrieveDataId(
			@Context final SecurityContext sc,
			@Context final Request request,
			@QueryParam(HttpHeaders.AUTHORIZATION) final String token,
			@HeaderParam("Range") final String range,
			@PathParam("oid") final ObjectId oid) throws FailException {
		final GenericContext gc = (GenericContext) sc.getUserPrincipal();
		LOGGER.debug("== DATA retrieveDataId ? oid={} user={}", oid, (gc == null ? "null" : gc.userByToken));
		final Data value = getSmall(oid);
		if (value == null) {
			return Response.status(404).entity("media NOT FOUND: " + oid).type("text/plain").build();
		}
		final EntityTag etag = etagOf(value);
		final Response notModified = notModifiedOrNull(request, etag);
		if (notModified != null) {
			return notModified;
		}
		try {
			return buildStream(getFileData(oid), range,
					value.getMimeType() == null ? "application/octet-stream" : value.getMimeType(), etag);
		} catch (final Exception ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Fail to build output stream", ex);
		}
	}

	/**
	 * Retrieves a thumbnail of the data identified by the given ObjectId, resizing images if possible.
	 * @param sc The security context.
	 * @param token Optional authorization token from query parameter.
	 * @param range The HTTP Range header value for partial content requests.
	 * @param oid The ObjectId of the data to thumbnail.
	 * @return A response containing the thumbnail image or the original stream.
	 * @throws FailException If the data is not found or processing fails.
	 */
	@GET
	@Path("thumbnail/{oid}")
	@RolesAllowed("USER")
	@PermitTokenInURI
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(description = "Get a thumbnail of from the data environment (if resize is possible)", tags = "SYSTEM")
	// @CacheMaxAge(time = 10, unit = TimeUnit.DAYS)
	public Response retrieveDataThumbnailId(
			@Context final SecurityContext sc,
			@Context final Request request,
			@QueryParam(HttpHeaders.AUTHORIZATION) final String token,
			@HeaderParam("Range") final String range,
			@PathParam("oid") final ObjectId oid) throws FailException {
		final GenericContext gc = (GenericContext) sc.getUserPrincipal();
		LOGGER.debug("== DATA retrieveDataThumbnailId ? {}", (gc == null ? "null" : gc.userByToken));
		final Data value = getSmall(oid);
		if (value == null) {
			return Response.status(404).entity("media NOT FOUND: " + oid).type("text/plain").build();
		}
		// A thumbnail is not the data it is made of: it deserves a name of its own, or the
		// browser would hand back the small picture when the whole one is asked for.
		final EntityTag etag = value.getSha512() == null ? etagOf(value)
				: new EntityTag("thumbnail-" + value.getSha512());
		final Response notModified = notModifiedOrNull(request, etag);
		if (notModified != null) {
			return notModified;
		}
		final String filePathName = getFileData(oid);
		final File inputFile = new File(filePathName);
		if (!inputFile.exists()) {
			return Response.status(404).entity("{\"error\":\"media Does not exist: " + oid + "\"}")
					.type("application/json").build();
		}
		if (value.getMimeType().contentEquals("image/jpeg") || value.getMimeType().contentEquals("image/png")
		// || value.mimeType.contentEquals("image/webp")
		) {
			// reads input image
			BufferedImage inputImage;
			try {
				inputImage = ImageIO.read(inputFile);
			} catch (final IOException ex) {
				throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Fail to READ the image", ex);
			}
			LOGGER.debug("input size image: {}x{} type={}", inputImage.getWidth(), inputImage.getHeight(),
					inputImage.getType());
			final int scaledWidth = ConfigBaseVariable.getThumbnailWidth();
			final int scaledHeight = (int) ((float) inputImage.getHeight() / (float) inputImage.getWidth()
					* scaledWidth);
			// creates output image
			final BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType());

			// scales the input image to the output image
			final Graphics2D g2d = outputImage.createGraphics();
			LOGGER.debug("output size image: {}x{}", scaledWidth, scaledHeight);
			g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
			g2d.dispose();
			// create the output stream:
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ImageIO.write(outputImage, ConfigBaseVariable.getThumbnailFormat(), baos);
			} catch (final IOException e) {
				LOGGER.error("Failed to write thumbnail image: {}", e.getMessage(), e);
				return Response.status(500).entity("Internal Error: thumbnail generation failed").type("text/plain")
						.build();
			}
			final byte[] imageData = baos.toByteArray();
			LOGGER.debug("output length {}", imageData.length);
			if (imageData.length == 0) {
				LOGGER.error("Fail to convert image... Availlable format:");
				for (final String data : ImageIO.getWriterFormatNames()) {
					LOGGER.error("    - {}", data);
				}
			}
			final Response.ResponseBuilder out = Response.ok(imageData).header(HttpHeaders.CONTENT_LENGTH,
					imageData.length);
			try {
				out.type(getMimeType(ConfigBaseVariable.getThumbnailFormat()));
			} catch (final IOException ex) {
				throw new FailException(Response.Status.INTERNAL_SERVER_ERROR,
						"Fail to convert mime type of " + ConfigBaseVariable.getThumbnailFormat(), ex);
			}
			// A thumbnail is made again from scratch on every request: without this a list of
			// covers asks for every picture again on every render, and the server spends its
			// time resizing them.
			addCacheHeaders(out, etag);
			return out.build();
		}
		try {
			return buildStream(filePathName, range, value.getMimeType(), etag);
		} catch (final Exception ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Fail to build output stream", ex);
		}
	}

	/**
	 * Retrieves data by its ObjectId with a user-friendly file name in the URL path (for browser downloads).
	 * @param sc The security context.
	 * @param token Optional authorization token from query parameter.
	 * @param range The HTTP Range header value for partial content requests.
	 * @param oid The ObjectId of the data to retrieve.
	 * @param name The display name for the downloaded file.
	 * @return A streaming response with the data content.
	 * @throws Exception If the data is not found or the stream cannot be built.
	 */
	@GET
	@Path("{oid}/{name}")
	@PermitTokenInURI
	@RolesAllowed("USER")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(description = "Get back some data from the data environment (with a beautiful name (permit download with basic name)", tags = "SYSTEM")
	public Response retrieveDataFull(
			@Context final SecurityContext sc,
			@Context final Request request,
			@QueryParam(HttpHeaders.AUTHORIZATION) final String token,
			@ApiInputOptional @HeaderParam("Range") final String range,
			@PathParam("oid") final ObjectId oid,
			@PathParam("name") final String name) throws Exception {
		final GenericContext gc = (GenericContext) sc.getUserPrincipal();
		LOGGER.debug("== DATA retrieveDataFull ? id={} user={}", oid, (gc == null ? "null" : gc.userByToken));
		final Data value = getSmall(oid);
		if (value == null) {
			return Response.status(404).entity("media NOT FOUND: " + oid).type("text/plain").build();
		}
		final EntityTag etag = etagOf(value);
		final Response notModified = notModifiedOrNull(request, etag);
		if (notModified != null) {
			return notModified;
		}
		return buildStream(getFileData(oid), range,
				value.getMimeType() == null ? "application/octet-stream" : value.getMimeType(), etag);
	}

	/** The tag of a data, as strong as its content: the same content is the same data.
	 *
	 * @param value The data to name.
	 * @return Its tag, or null when it has nothing to be named by. */
	private static EntityTag etagOf(final Data value) {
		if (value == null) {
			return null;
		}
		if (value.getSha512() != null) {
			return new EntityTag(value.getSha512());
		}
		if (value.getOid() != null) {
			return new EntityTag(value.getOid().toHexString());
		}
		return null;
	}

	/** Answer with nothing when the browser already holds this exact content.
	 *
	 * @param request The incoming request, holding what the browser already has.
	 * @param etag The tag of what is being asked for.
	 * @return The answer to send back, or null when the content has to be sent. */
	private static Response notModifiedOrNull(final Request request, final EntityTag etag) {
		if (request == null || etag == null) {
			return null;
		}
		final Response.ResponseBuilder builder = request.evaluatePreconditions(etag);
		if (builder == null) {
			return null;
		}
		return builder.header(HttpHeaders.CACHE_CONTROL, DATA_CACHE_CONTROL).tag(etag).build();
	}

	/** Say how long this may be kept, and by what name.
	 *
	 * @param builder The answer being built.
	 * @param etag The tag of the content, null when it has none. */
	private static void addCacheHeaders(final Response.ResponseBuilder builder, final EntityTag etag) {
		builder.header(HttpHeaders.CACHE_CONTROL, DATA_CACHE_CONTROL);
		if (etag != null) {
			builder.tag(etag);
		}
	}

	/** What a "Range" header asks for, once read.
	 *
	 * @param from First byte asked for.
	 * @param to Last byte asked for, included. */
	public record ByteRange(
			long from,
			long to) {}

	/** A range that says nothing this file can answer: it starts past the end. */
	public static final ByteRange RANGE_OUT_OF_FILE = new ByteRange(-1, -1);

	/** Read a "Range" header.
	 *
	 * <p>
	 * Only the first range of a list is honoured: answering several at once needs a multipart body,
	 * and no player asks for that.
	 *
	 * @param range The header, "bytes=0-1023", "bytes=1024-" or "bytes=-1024".
	 * @param fileLength The length of the file being asked about.
	 * @return What to send back, {@link #RANGE_OUT_OF_FILE} when it asks past the end of the file,
	 *         or null when there is nothing to read in it and the whole file is to be sent. */
	public static ByteRange parseRange(final String range, final long fileLength) {
		if (range == null || fileLength <= 0) {
			return null;
		}
		final int equalPos = range.indexOf('=');
		if (equalPos < 0 || !"bytes".equalsIgnoreCase(range.substring(0, equalPos).trim())) {
			return null;
		}
		// Several ranges at once would need a multipart answer: the first one is served, which
		// every player accepts.
		String first = range.substring(equalPos + 1).trim();
		final int commaPos = first.indexOf(',');
		if (commaPos >= 0) {
			first = first.substring(0, commaPos).trim();
		}
		final int dashPos = first.indexOf('-');
		if (dashPos < 0) {
			return null;
		}
		final String startText = first.substring(0, dashPos).trim();
		final String endText = first.substring(dashPos + 1).trim();
		try {
			if (startText.isEmpty()) {
				// "bytes=-N": the last N bytes. A player reads the index of a Matroska this way,
				// and without an answer to it seeking in a media never works.
				if (endText.isEmpty()) {
					return null;
				}
				final long wanted = Long.parseLong(endText);
				if (wanted <= 0) {
					return RANGE_OUT_OF_FILE;
				}
				final long from = Math.max(0, fileLength - wanted);
				return new ByteRange(from, fileLength - 1);
			}
			final long from = Long.parseLong(startText);
			if (from < 0 || from >= fileLength) {
				return RANGE_OUT_OF_FILE;
			}
			long to = endText.isEmpty() ? fileLength - 1 : Long.parseLong(endText);
			if (to >= fileLength) {
				to = fileLength - 1;
			}
			if (to < from) {
				return RANGE_OUT_OF_FILE;
			}
			return new ByteRange(from, to);
		} catch (final NumberFormatException ex) {
			LOGGER.warn("Can not read the range '{}': {}", range, ex.getMessage());
			return null;
		}
	}

	/** Adapted from http://stackoverflow.com/questions/12768812/video-streaming-to-ipad-does-not-work-with-tapestry5/12829541#12829541
	 *
	 * @param range range header
	 * @return Streaming output
	 * @throws FileNotFoundException
	 * @throws Exception IOException if an error occurs in streaming. */
	private Response buildStream(final String filename, final String range, final String inputMimeType)
			throws FailException {
		return buildStream(filename, range, inputMimeType, null);
	}

	/** Send a file back, whole or in part.
	 *
	 * @param filename The file on disk.
	 * @param range What the browser asks for, null when it asks for everything.
	 * @param inputMimeType The type of the content, as it is stored.
	 * @param etag The tag of the content, so the browser can keep it.
	 * @return The answer to send back.
	 * @throws FailException When the file can not be read. */
	private Response buildStream(
			final String filename,
			final String range,
			final String inputMimeType,
			final EntityTag etag) throws FailException {
		// Browsers don't support video/x-matroska or audio/x-matroska, serve as webm instead
		final String mimeType;
		if ("video/x-matroska".equals(inputMimeType)) {
			mimeType = "video/webm";
		} else if ("audio/x-matroska".equals(inputMimeType)) {
			mimeType = "audio/webm";
		} else {
			mimeType = inputMimeType;
		}
		final File file = new File(filename);
		final ByteRange asked = parseRange(range, file.length());
		if (asked == RANGE_OUT_OF_FILE) {
			// Sending the whole file back would have the player read, at great length, something
			// other than what it asked for.
			return Response.status(Response.Status.REQUESTED_RANGE_NOT_SATISFIABLE).header("Accept-Ranges", "bytes")
					.header("Content-Range", "bytes */" + file.length()).build();
		}
		// range not requested : Firefox does not send range headers
		if (asked == null) {
			final StreamingOutput output = new StreamingOutput() {
				@Override
				public void write(final OutputStream out) {
					try (FileInputStream in = new FileInputStream(file)) {
						final byte[] buf = new byte[CHUNK_SIZE];
						int len;
						while ((len = in.read(buf)) != -1) {
							try {
								out.write(buf, 0, len);
								// logger.info("---- wrote {} bytes file ----", len);
							} catch (final IOException ex) {
								LOGGER.info("remote close connection");
								break;
							}
						}
						out.flush();
					} catch (final IOException ex) {
						throw new InternalServerErrorException(ex);
					}
				}
			};
			final Response.ResponseBuilder out = Response.ok(output).header(HttpHeaders.CONTENT_LENGTH, file.length())
					.header("Accept-Ranges", "bytes").header(HttpHeaders.LAST_MODIFIED, new Date(file.lastModified()));
			addCacheHeaders(out, etag);
			if (mimeType != null) {
				out.type(mimeType);
			}
			return out.build();

		}

		final long from = asked.from();
		final long to = asked.to();
		final String responseRange = String.format("bytes %d-%d/%d", from, to, file.length());
		// LOGGER.info("responseRange: {}", responseRange);
		try {
			final RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(from);

			final long len = to - from + 1;
			final MediaStreamer streamer = new MediaStreamer(len, raf);
			final Response.ResponseBuilder out = Response.ok(streamer).status(Response.Status.PARTIAL_CONTENT)
					.header("Accept-Ranges", "bytes").header("Content-Range", responseRange)
					.header(HttpHeaders.CONTENT_LENGTH, streamer.getLenth())
					.header(HttpHeaders.LAST_MODIFIED, new Date(file.lastModified()));
			addCacheHeaders(out, etag);
			if (mimeType != null) {
				out.type(mimeType);
			}
			return out.build();
		} catch (final FileNotFoundException ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Fail to find the required file.", ex);
		} catch (final IOException ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Fail to access to the required file.", ex);
		}
	}

	/**
	 * Restores a soft-deleted {@link Data} record by its numeric identifier.
	 * @param id The numeric identifier of the record to restore.
	 * @throws Exception If the restore operation fails.
	 */
	public void undelete(final Long id) throws Exception {
		DataAccess.restoreById(Data.class, id);
	}

}
