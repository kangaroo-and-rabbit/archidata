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
import org.atriasoft.archidata.dataAccess.QueryCondition;
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
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.StreamingOutput;

// https://stackoverflow.com/questions/35367113/jersey-webservice-scalable-approach-to-download-file-and-reply-to-client
// https://gist.github.com/aitoroses/4f7a2b197b732a6a691d

@Path("/data")
@Produces(MediaType.APPLICATION_JSON)
public class DataResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(DataResource.class);
	private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
	private static final int CHUNK_SIZE_IN = 50 * 1024 * 1024; // 1MB chunks
	/** Upload some datas */
	private static long tmpFolderId = 1;

	private static void createFolder(final String path) throws IOException {
		if (!Files.exists(java.nio.file.Path.of(path))) {
			// Log.print("Create folder: " + path);
			Files.createDirectories(java.nio.file.Path.of(path));
		}
	}

	public static long getTmpDataId() {
		return tmpFolderId++;
	}

	public static String getTmpFileInData(final long tmpFolderId) {
		final String filePath = ConfigBaseVariable.getTmpDataFolder() + File.separator + tmpFolderId;
		try {
			createFolder(ConfigBaseVariable.getTmpDataFolder() + File.separator);
		} catch (final IOException e) {
			LOGGER.error("Failed to create tmp data folder: {}", e.getMessage(), e);
		}
		return filePath;
	}

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

	public static String getFileMetaData(final ObjectId oid) {
		return getFileData(oid) + ".json";
	}

	public Data getWithSha512(final String sha512) {
		LOGGER.info("find sha512 = {}", sha512);
		try {
			return DataAccess.get(Data.class, new Condition(new QueryCondition("sha512", "=", sha512)));
		} catch (final Exception e) {
			LOGGER.error("Failed to get data with sha512: {}", e.getMessage(), e);
		}
		return null;
	}

	public Data getWithId(final long id) {
		LOGGER.info("find id = {}", id);
		try {
			return DataAccess.getById(Data.class, id);
		} catch (final Exception e) {
			LOGGER.error("Failed to get data with id: {}", e.getMessage(), e);
		}
		return null;
	}

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

	public Data createNewData(final long tmpUID, final String originalFileName, final String sha512)
			throws IOException {
		// determine mime type:
		Data injectedData = new Data();
		String mimeType = "";
		final String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1);
		mimeType = getMimeType(extension);
		injectedData.mimeType = mimeType;
		injectedData.sha512 = sha512;
		final String tmpPath = getTmpFileInData(tmpUID);
		injectedData.size = Files.size(Paths.get(tmpPath));

		try {
			injectedData = DataAccess.insert(injectedData);
		} catch (final Exception e) {
			LOGGER.error("Failed to insert data: {}", e.getMessage(), e);
			return null;
		}
		final String mediaPath = getFileData(injectedData.oid);
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

	public static String saveTemporaryFile(final InputStream uploadedInputStream, final long idData)
			throws FailException {
		return saveFile(uploadedInputStream, DataResource.getTmpFileInData(idData));
	}

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

	public static String bytesToHex(final byte[] bytes) {
		final StringBuilder sb = new StringBuilder();
		for (final byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	public Data getSmall(final ObjectId oid) {
		try {
			return DataAccess.getById(Data.class, oid);
		} catch (final Exception e) {
			LOGGER.error("Failed to get data by OID: {}", e.getMessage(), e);
		}
		return null;
	}

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

	@GET
	@Path("{oid}")
	@PermitTokenInURI
	@RolesAllowed("USER")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(description = "Get back some data from the data environment", tags = "SYSTEM")
	public Response retrieveDataId(
			@Context final SecurityContext sc,
			@QueryParam(HttpHeaders.AUTHORIZATION) final String token,
			@HeaderParam("Range") final String range,
			@PathParam("oid") final ObjectId oid) throws FailException {
		final GenericContext gc = (GenericContext) sc.getUserPrincipal();
		// logger.info("===================================================");
		LOGGER.info("== DATA retrieveDataId ? oid={} user={}", oid, (gc == null ? "null" : gc.userByToken));
		// logger.info("===================================================");
		final Data value = getSmall(oid);
		if (value == null) {
			return Response.status(404).entity("media NOT FOUND: " + oid).type("text/plain").build();
		}
		try {
			return buildStream(getFileData(oid), range,
					value.mimeType == null ? "application/octet-stream" : value.mimeType);
		} catch (final Exception ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Fail to build output stream", ex);
		}
	}

	@GET
	@Path("thumbnail/{oid}")
	@RolesAllowed("USER")
	@PermitTokenInURI
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(description = "Get a thumbnail of from the data environment (if resize is possible)", tags = "SYSTEM")
	// @CacheMaxAge(time = 10, unit = TimeUnit.DAYS)
	public Response retrieveDataThumbnailId(
			@Context final SecurityContext sc,
			@QueryParam(HttpHeaders.AUTHORIZATION) final String token,
			@HeaderParam("Range") final String range,
			@PathParam("oid") final ObjectId oid) throws FailException {
		final GenericContext gc = (GenericContext) sc.getUserPrincipal();
		LOGGER.info("===================================================");
		LOGGER.info("== DATA retrieveDataThumbnailId ? {}", (gc == null ? "null" : gc.userByToken));
		LOGGER.info("===================================================");
		final Data value = getSmall(oid);
		if (value == null) {
			return Response.status(404).entity("media NOT FOUND: " + oid).type("text/plain").build();
		}
		final String filePathName = getFileData(oid);
		final File inputFile = new File(filePathName);
		if (!inputFile.exists()) {
			return Response.status(404).entity("{\"error\":\"media Does not exist: " + oid + "\"}")
					.type("application/json").build();
		}
		if (value.mimeType.contentEquals("image/jpeg") || value.mimeType.contentEquals("image/png")
		// || value.mimeType.contentEquals("image/webp")
		) {
			// reads input image
			BufferedImage inputImage;
			try {
				inputImage = ImageIO.read(inputFile);
			} catch (final IOException ex) {
				throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Fail to READ the image", ex);
			}
			LOGGER.info("input size image: {}x{} type={}", inputImage.getWidth(), inputImage.getHeight(),
					inputImage.getType());
			final int scaledWidth = ConfigBaseVariable.getThumbnailWidth();
			final int scaledHeight = (int) ((float) inputImage.getHeight() / (float) inputImage.getWidth()
					* scaledWidth);
			// creates output image
			final BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType());

			// scales the input image to the output image
			final Graphics2D g2d = outputImage.createGraphics();
			LOGGER.info("output size image: {}x{}", scaledWidth, scaledHeight);
			g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
			g2d.dispose();
			// create the output stream:
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				ImageIO.write(outputImage, ConfigBaseVariable.getThumbnailFormat(), baos);
			} catch (final IOException e) {
				LOGGER.error("Failed to write thumbnail image: {}", e.getMessage(), e);
				return Response.status(500).entity("Internal Error: resize fail: " + e.getMessage()).type("text/plain")
						.build();
			}
			final byte[] imageData = baos.toByteArray();
			LOGGER.info("output length {}", imageData.length);
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
			// TODO: move this in a decorator !!!
			final CacheControl cc = new CacheControl();
			cc.setMaxAge(3600);
			cc.setNoCache(false);
			out.cacheControl(cc);
			return out.build();
		}
		try {
			return buildStream(filePathName, range, value.mimeType);
		} catch (final Exception ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Fail to build output stream", ex);
		}
	}

	@GET
	@Path("{oid}/{name}")
	@PermitTokenInURI
	@RolesAllowed("USER")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(description = "Get back some data from the data environment (with a beautiful name (permit download with basic name)", tags = "SYSTEM")
	public Response retrieveDataFull(
			@Context final SecurityContext sc,
			@QueryParam(HttpHeaders.AUTHORIZATION) final String token,
			@ApiInputOptional @HeaderParam("Range") final String range,
			@PathParam("oid") final ObjectId oid,
			@PathParam("name") final String name) throws Exception {
		final GenericContext gc = (GenericContext) sc.getUserPrincipal();
		// logger.info("===================================================");
		LOGGER.info("== DATA retrieveDataFull ? id={} user={}", oid, (gc == null ? "null" : gc.userByToken));
		// logger.info("===================================================");
		final Data value = getSmall(oid);
		if (value == null) {
			return Response.status(404).entity("media NOT FOUND: " + oid).type("text/plain").build();
		}
		return buildStream(getFileData(oid), range,
				value.mimeType == null ? "application/octet-stream" : value.mimeType);
	}

	/** Adapted from http://stackoverflow.com/questions/12768812/video-streaming-to-ipad-does-not-work-with-tapestry5/12829541#12829541
	 *
	 * @param range range header
	 * @return Streaming output
	 * @throws FileNotFoundException
	 * @throws Exception IOException if an error occurs in streaming. */
	private Response buildStream(final String filename, final String range, final String mimeType)
			throws FailException {
		final File file = new File(filename);
		// logger.info("request range : {}", range);
		// range not requested : Firefox does not send range headers
		if (range == null) {
			final StreamingOutput output = new StreamingOutput() {
				@Override
				public void write(final OutputStream out) {
					try (FileInputStream in = new FileInputStream(file)) {
						final byte[] buf = new byte[1024 * 1024];
						int len;
						while ((len = in.read(buf)) != -1) {
							try {
								out.write(buf, 0, len);
								out.flush();
								// logger.info("---- wrote {} bytes file ----", len);
							} catch (final IOException ex) {
								LOGGER.info("remote close connection");
								break;
							}
						}
					} catch (final IOException ex) {
						throw new InternalServerErrorException(ex);
					}
				}
			};
			final Response.ResponseBuilder out = Response.ok(output).header(HttpHeaders.CONTENT_LENGTH, file.length());
			if (mimeType != null) {
				out.type(mimeType);
			}
			return out.build();

		}

		final String[] ranges = range.split("=")[1].split("-");
		final long from = Long.parseLong(ranges[0]);

		// logger.info("request range : {}", ranges.length);
		// Chunk media if the range upper bound is unspecified. Chrome, Opera sends "bytes=0-"
		long to = CHUNK_SIZE + from;
		if (ranges.length == 1) {
			to = file.length() - 1;
		} else if (to >= file.length()) {
			to = file.length() - 1;
		}
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

	public void undelete(final Long id) throws Exception {
		DataAccess.restoreById(Data.class, id);
	}

}
