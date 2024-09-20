package org.kar.archidata.api;

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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.kar.archidata.annotation.security.PermitTokenInURI;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.QueryCondition;
import org.kar.archidata.dataAccess.options.Condition;
import org.kar.archidata.exception.FailException;
import org.kar.archidata.filter.GenericContext;
import org.kar.archidata.model.Data;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.RolesAllowed;
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
	private final static int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
	private final static int CHUNK_SIZE_IN = 50 * 1024 * 1024; // 1MB chunks
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
			e.printStackTrace();
		}
		return filePath;
	}

	public static String getFileDataOld(final long tmpFolderId) {
		final String filePath = ConfigBaseVariable.getMediaDataFolder() + File.separator + tmpFolderId + File.separator
				+ "data";
		try {
			createFolder(ConfigBaseVariable.getMediaDataFolder() + File.separator + tmpFolderId + File.separator);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return filePath;
	}

	public static String getFileData(final UUID uuid) {
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
			e.printStackTrace();
		}
		filePath += part3;
		return filePath;
	}

	public static String getFileMetaData(final UUID uuid) {
		return getFileData(uuid) + ".json";
	}

	public static Data getWithSha512(final String sha512) {
		LOGGER.info("find sha512 = {}", sha512);
		try {
			return DataAccess.getWhere(Data.class, new Condition(new QueryCondition("sha512", "=", sha512)));
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static Data getWithId(final long id) {
		LOGGER.info("find id = {}", id);
		try {
			return DataAccess.get(Data.class, id);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static Data createNewData(final long tmpUID, final String originalFileName, final String sha512)
			throws IOException {
		// determine mime type:
		Data injectedData = new Data();
		String mimeType = "";
		final String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1);
		mimeType = switch (extension.toLowerCase()) {
			case "jpg", "jpeg" -> "image/jpeg";
			case "png" -> "image/png";
			case "webp" -> "image/webp";
			case "mka" -> "audio/x-matroska";
			case "mkv" -> "video/x-matroska";
			case "webm" -> "video/webm";
			default -> throw new IOException("Can not find the mime type of data input: '" + extension + "'");
		};
		injectedData.mimeType = mimeType;
		injectedData.sha512 = sha512;
		final String tmpPath = getTmpFileInData(tmpUID);
		injectedData.size = Files.size(Paths.get(tmpPath));

		try {
			injectedData = DataAccess.insert(injectedData);
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
		final String mediaPath = getFileData(injectedData.uuid);
		LOGGER.info("src = {}", tmpPath);
		LOGGER.info("dst = {}", mediaPath);
		Files.move(Paths.get(tmpPath), Paths.get(mediaPath), StandardCopyOption.ATOMIC_MOVE);
		LOGGER.info("Move done");
		return injectedData;
	}

	public static void modeFileOldModelToNewModel(final long id, final UUID uuid) throws IOException {
		String mediaCurentPath = getFileDataOld(id);
		String mediaDestPath = getFileData(uuid);
		LOGGER.info("src = {}", mediaCurentPath);
		LOGGER.info("dst = {}", mediaDestPath);
		if (Files.exists(Paths.get(mediaCurentPath))) {
			LOGGER.info("move: {} ==> {}", mediaCurentPath, mediaDestPath);
			Files.move(Paths.get(mediaCurentPath), Paths.get(mediaDestPath), StandardCopyOption.ATOMIC_MOVE);
		}
		// Move old meta-data...
		mediaCurentPath = mediaCurentPath.substring(mediaCurentPath.length() - 4) + "meta.json";
		mediaDestPath = mediaCurentPath.substring(mediaDestPath.length() - 4) + "meta.json";
		if (Files.exists(Paths.get(mediaCurentPath))) {
			LOGGER.info("moveM: {} ==> {}", mediaCurentPath, mediaDestPath);
			Files.move(Paths.get(mediaCurentPath), Paths.get(mediaDestPath), StandardCopyOption.ATOMIC_MOVE);
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
				LOGGER.info("can not delete temporary file : {}", Paths.get(filepath));
				e.printStackTrace();
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

	public Data getSmall(final UUID id) {
		try {
			return DataAccess.get(Data.class, id);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@POST
	@Path("/upload/")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@RolesAllowed("ADMIN")
	@Operation(description = "Insert a new data in the data environment", tags = "SYSTEM")
	public void uploadFile(
			@Context final SecurityContext sc,
			@FormDataParam("file") final InputStream fileInputStream,
			@FormDataParam("file") final FormDataContentDisposition fileMetaData) throws FailException {
		final GenericContext gc = (GenericContext) sc.getUserPrincipal();
		LOGGER.info("===================================================");
		LOGGER.info("== DATA uploadFile {}", (gc == null ? "null" : gc.userByToken));
		LOGGER.info("===================================================");
		// public NodeSmall uploadFile(final FormDataMultiPart form) {
		LOGGER.info("Upload file: ");
		final String filePath = ConfigBaseVariable.getTmpDataFolder() + File.separator + tmpFolderId++;
		try {
			createFolder(ConfigBaseVariable.getTmpDataFolder() + File.separator);
		} catch (final IOException ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR,
					"Impossible to create the folder in the server", ex);
		}
		saveFile(fileInputStream, filePath);
	}

	@GET
	@Path("{uuid}")
	@PermitTokenInURI
	@RolesAllowed("USER")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(description = "Get back some data from the data environment", tags = "SYSTEM")
	public Response retrieveDataId(
			@Context final SecurityContext sc,
			@QueryParam(HttpHeaders.AUTHORIZATION) final String token,
			@HeaderParam("Range") final String range,
			@PathParam("uuid") final UUID uuid) throws FailException {
		final GenericContext gc = (GenericContext) sc.getUserPrincipal();
		// logger.info("===================================================");
		LOGGER.info("== DATA retrieveDataId ? id={} user={}", uuid, (gc == null ? "null" : gc.userByToken));
		// logger.info("===================================================");
		final Data value = getSmall(uuid);
		if (value == null) {
			LOGGER.warn("Request data that does not exist : {}", uuid);
			return Response.status(404).entity("media NOT FOUND: " + uuid).type("text/plain").build();
		}
		try {
			LOGGER.warn("Generate stream : {}", uuid);
			return buildStream(getFileData(uuid), range,
					value.mimeType == null ? "application/octet-stream" : value.mimeType);
		} catch (final Exception ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Fail to build output stream", ex);
		}
	}

	@GET
	@Path("thumbnail/{uuid}")
	@RolesAllowed("USER")
	@PermitTokenInURI
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(description = "Get a thumbnail of from the data environment (if resize is possible)", tags = "SYSTEM")
	// @CacheMaxAge(time = 10, unit = TimeUnit.DAYS)
	public Response retrieveDataThumbnailId(
			@Context final SecurityContext sc,
			@QueryParam(HttpHeaders.AUTHORIZATION) final String token,
			@HeaderParam("Range") final String range,
			@PathParam("uuid") final UUID uuid) throws FailException {
		final GenericContext gc = (GenericContext) sc.getUserPrincipal();
		LOGGER.info("===================================================");
		LOGGER.info("== DATA retrieveDataThumbnailId ? {}", (gc == null ? "null" : gc.userByToken));
		LOGGER.info("===================================================");
		final Data value = getSmall(uuid);
		if (value == null) {
			return Response.status(404).entity("media NOT FOUND: " + uuid).type("text/plain").build();
		}
		final String filePathName = getFileData(uuid);
		final File inputFile = new File(filePathName);
		if (!inputFile.exists()) {
			return Response.status(404).entity("{\"error\":\"media Does not exist: " + uuid + "\"}")
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
			final int scaledWidth = 250;
			final int scaledHeight = (int) ((float) inputImage.getHeight() / (float) inputImage.getWidth()
					* scaledWidth);

			// creates output image
			final BufferedImage outputImage = new BufferedImage(scaledWidth, scaledHeight, inputImage.getType());

			// scales the input image to the output image
			final Graphics2D g2d = outputImage.createGraphics();
			LOGGER.info("output size image: {}x{}", scaledWidth, scaledHeight);
			g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
			g2d.dispose();
			for (final String data : ImageIO.getWriterFormatNames()) {
				LOGGER.info("availlable format: {}", data);
			}
			// create the output stream:
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				// TODO: check how to remove buffer file !!! here, it is not needed at all...
				//ImageIO.write(outputImage, "JPEG", baos);
				//ImageIO.write(outputImage, "png", baos);
				ImageIO.write(outputImage, "WebP", baos);
			} catch (final IOException e) {
				e.printStackTrace();
				return Response.status(500).entity("Internal Error: resize fail: " + e.getMessage()).type("text/plain")
						.build();
			}
			final byte[] imageData = baos.toByteArray();
			LOGGER.info("output length {}", imageData.length);
			// Response.ok(new ByteArrayInputStream(imageData)).build();
			final Response.ResponseBuilder out = Response.ok(imageData).header(HttpHeaders.CONTENT_LENGTH,
					imageData.length);
			//out.type("image/jpeg");
			out.type("image/webp");
			//out.type("image/png");
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
	@Path("{uuid}/{name}")
	@PermitTokenInURI
	@RolesAllowed("USER")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@Operation(description = "Get back some data from the data environment (with a beautiful name (permit download with basic name)", tags = "SYSTEM")
	public Response retrieveDataFull(
			@Context final SecurityContext sc,
			@QueryParam(HttpHeaders.AUTHORIZATION) final String token,
			@HeaderParam("Range") final String range,
			@PathParam("uuid") final UUID uuid,
			@PathParam("name") final String name) throws Exception {
		final GenericContext gc = (GenericContext) sc.getUserPrincipal();
		// logger.info("===================================================");
		LOGGER.info("== DATA retrieveDataFull ? id={} user={}", uuid, (gc == null ? "null" : gc.userByToken));
		// logger.info("===================================================");
		final Data value = getSmall(uuid);
		if (value == null) {
			return Response.status(404).entity("media NOT FOUND: " + uuid).type("text/plain").build();
		}
		return buildStream(getFileData(uuid), range,
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

	public static void undelete(final Long id) throws Exception {
		DataAccess.unsetDelete(Data.class, id);
	}

}
