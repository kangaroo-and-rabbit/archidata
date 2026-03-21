package org.atriasoft.archidata.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.apache.tika.Tika;
import org.atriasoft.archidata.api.DataResource;
import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.commonTools.ListInDbTools;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.exception.FailException;
import org.atriasoft.archidata.exception.InputException;
import org.atriasoft.archidata.model.Data;
import org.bson.types.ObjectId;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.Filters;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

/**
 * Utility class for managing binary data (files, images, audio, video) storage.
 *
 * <p>Provides methods for uploading, saving, and retrieving binary data with SHA-512 checksum
 * deduplication. Files are first saved to a temporary location, then moved to their permanent
 * storage path once registered in the database.</p>
 */
public class DataTools {

	private DataTools() {
		// Utility class
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(DataTools.class);

	/** Read/write chunk size for streaming operations (1 MB). */
	public static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
	/** Input buffer size for file uploads (50 MB). */
	public static final int CHUNK_SIZE_IN = 50 * 1024 * 1024; // 50MB chunks
	/** Counter for generating unique temporary file identifiers. */
	private static long tmpFolderId = 1;
	public static final String[] SUPPORTED_IMAGE_MIME_TYPE = { "image/jpeg", "image/png", "image/webp" };
	public static final String[] SUPPORTED_AUDIO_MIME_TYPE = { "audio/x-matroska" };
	public static final String[] SUPPORTED_VIDEO_MIME_TYPE = { "video/x-matroska", "video/webm" };

	/**
	 * Creates a directory (and parent directories) if it does not already exist.
	 * @param path The directory path to create.
	 * @throws IOException If the directory cannot be created.
	 */
	public static void createFolder(final String path) throws IOException {
		if (!Files.exists(java.nio.file.Path.of(path))) {
			LOGGER.info("Create folder: {}", path);
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
	 * Returns a unique temporary folder path and creates the parent directory if needed.
	 * @return The absolute path to a new temporary folder.
	 */
	public static String getTmpFolder() {
		final String filePath = ConfigBaseVariable.getTmpDataFolder() + File.separator + tmpFolderId++;
		try {
			createFolder(ConfigBaseVariable.getTmpDataFolder() + File.separator);
		} catch (final IOException e) {
			LOGGER.error("Failed to create tmp folder: {}", e.getMessage(), e);
		}
		return filePath;
	}

	/**
	 * Retrieves a {@link Data} record matching the given SHA-512 hash.
	 * @param ioDb The database access instance.
	 * @param sha512 The SHA-512 hash to search for.
	 * @return The matching {@link Data} record, or {@code null} if not found.
	 */
	public static Data getWithSha512(final DBAccessMongo ioDb, final String sha512) {
		try {
			return ioDb.get(Data.class, new Condition(Filters.eq("sha512", sha512)), new ReadAllColumn());
		} catch (final Exception e) {
			LOGGER.error("Failed to get data with sha512: {}", e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Retrieves a non-deleted {@link Data} record by its numeric identifier.
	 * @param ioDb The database access instance.
	 * @param id The numeric identifier to search for.
	 * @return The matching {@link Data} record, or {@code null} if not found or deleted.
	 */
	public static Data getWithId(final DBAccessMongo ioDb, final long id) {
		try {
			return ioDb.get(Data.class, new Condition(Filters.and(Filters.eq("deleted", false), Filters.eq("id", id))));
		} catch (final Exception e) {
			LOGGER.error("Failed to get data with id: {}", e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Creates a new {@link Data} record in the database and moves the temporary file to permanent storage.
	 * @param ioDb The database access instance.
	 * @param tmpUID The temporary file identifier.
	 * @param originalFileName The original file name.
	 * @param sha512 The SHA-512 hash of the file content.
	 * @param mimeType The MIME type of the file.
	 * @return The newly created {@link Data} record, or {@code null} on insertion failure.
	 * @throws IOException If the file cannot be moved.
	 */
	public static Data createNewData(
			final DBAccessMongo ioDb,
			final long tmpUID,
			final String originalFileName,
			final String sha512,
			final String mimeType) throws IOException {

		final String tmpPath = getTmpFileInData(tmpUID);
		final long fileSize = Files.size(Paths.get(tmpPath));
		Data out = new Data();

		try {
			out.setSha512(sha512);
			out.setMimeType(mimeType);
			out.setSize(fileSize);
			out = ioDb.insert(out);
		} catch (final Exception e) {
			LOGGER.error("Failed to insert data: {}", e.getMessage(), e);
			return null;
		}

		final String mediaPath = DataResource.getFileData(out.getOid());
		LOGGER.info("src = {}", tmpPath);
		LOGGER.info("dst = {}", mediaPath);
		try {
			Files.move(Paths.get(tmpPath), Paths.get(mediaPath), StandardCopyOption.ATOMIC_MOVE);
			LOGGER.info("Atomic-move done");
		} catch (final AtomicMoveNotSupportedException ex) {
			Files.move(Paths.get(tmpPath), Paths.get(mediaPath), StandardCopyOption.REPLACE_EXISTING);
			LOGGER.info("Move done");
		}
		// all is done the file is correctly installed...
		return out;
	}

	/**
	 * Creates a new {@link Data} record, determining the MIME type from the file extension.
	 * @param ioDb The database access instance.
	 * @param tmpUID The temporary file identifier.
	 * @param originalFileName The original file name (used to determine MIME type).
	 * @param sha512 The SHA-512 hash of the file content.
	 * @return The newly created {@link Data} record.
	 * @throws IOException If the MIME type cannot be determined or the file cannot be moved.
	 */
	public static Data createNewData(
			final DBAccessMongo ioDb,
			final long tmpUID,
			final String originalFileName,
			final String sha512) throws IOException {
		// determine mime type:
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
		return createNewData(ioDb, tmpUID, originalFileName, sha512, mimeType);
	}

	/**
	 * Restores a soft-deleted {@link Data} record by its ObjectId.
	 * @param ioDb The database access instance.
	 * @param oid The ObjectId of the record to restore.
	 */
	public static void undelete(final DBAccessMongo ioDb, final ObjectId oid) {
		try {
			ioDb.restoreById(Data.class, oid);
		} catch (final Exception e) {
			LOGGER.error("Failed to restore data: {}", e.getMessage(), e);
		}
	}

	/**
	 * Saves an input stream to a temporary file and returns its SHA-512 hash.
	 * @param uploadedInputStream The input stream to save.
	 * @param idData The temporary file identifier.
	 * @return The SHA-512 hex string of the saved data.
	 */
	public static String saveTemporaryFile(final InputStream uploadedInputStream, final long idData) {
		return saveFile(uploadedInputStream, getTmpFileInData(idData));
	}

	/**
	 * Saves a byte array to a temporary file and returns its SHA-512 hash.
	 * @param uploadedInputStream The byte array to save.
	 * @param idData The temporary file identifier.
	 * @return The SHA-512 hex string of the saved data.
	 */
	public static String saveTemporaryFile(final byte[] uploadedInputStream, final long idData) {
		return saveFile(uploadedInputStream, getTmpFileInData(idData));
	}

	/**
	 * Deletes a temporary file if it exists.
	 * @param idData The temporary file identifier.
	 */
	public static void removeTemporaryFile(final long idData) {
		final String filepath = getTmpFileInData(idData);
		if (Files.exists(Paths.get(filepath))) {
			try {
				Files.delete(Paths.get(filepath));
			} catch (final IOException e) {
				LOGGER.error("Can not delete temporary file '{}': {}", Paths.get(filepath), e.getMessage(), e);
			}
		}
	}

	/**
	 * Saves an input stream to a file and computes its SHA-512 hash.
	 * @param uploadedInputStream The input stream to save.
	 * @param serverLocation The destination file path.
	 * @return The SHA-512 hex string of the saved data, or an empty string on error.
	 */
	public static String saveFile(final InputStream uploadedInputStream, final String serverLocation) {
		String out = "";
		try {
			int read = 0;
			final byte[] bytes = new byte[CHUNK_SIZE_IN];
			final MessageDigest md = MessageDigest.getInstance("SHA-512");
			try (final OutputStream outputStream = new FileOutputStream(new File(serverLocation))) {
				while ((read = uploadedInputStream.read(bytes)) != -1) {
					md.update(bytes, 0, read);
					outputStream.write(bytes, 0, read);
				}
				LOGGER.info("Flush input stream ... {}", serverLocation);
				outputStream.flush();
			}
			// create the end of sha512
			final byte[] sha512Digest = md.digest();
			// convert in hexadecimal
			out = bytesToHex(sha512Digest);
			uploadedInputStream.close();
		} catch (final IOException ex) {
			LOGGER.error("Can not write in temporary file: {}", ex.getMessage(), ex);
		} catch (final NoSuchAlgorithmException ex) {
			LOGGER.error("Can not find sha512 algorithms: {}", ex.getMessage(), ex);
		}
		return out;
	}

	/**
	 * Saves a byte array to a file and computes its SHA-512 hash.
	 * @param bytes The byte array to save.
	 * @param serverLocation The destination file path.
	 * @return The SHA-512 hex string of the saved data, or an empty string on error.
	 */
	public static String saveFile(final byte[] bytes, final String serverLocation) {
		String out = "";
		try {
			final MessageDigest md = MessageDigest.getInstance("SHA-512");
			try (final OutputStream outputStream = new FileOutputStream(new File(serverLocation))) {
				md.update(bytes, 0, bytes.length);
				outputStream.write(bytes, 0, bytes.length);
				LOGGER.info("Flush input stream ... {}", serverLocation);
				outputStream.flush();
			}
			// create the end of sha512
			final byte[] sha512Digest = md.digest();
			// convert in hexadecimal
			out = bytesToHex(sha512Digest);
		} catch (final IOException ex) {
			LOGGER.error("Can not write in temporary file: {}", ex.getMessage(), ex);
		} catch (final NoSuchAlgorithmException ex) {
			LOGGER.error("Can not find sha512 algorithms: {}", ex.getMessage(), ex);
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
	 * Normalizes multipart form data values by converting empty, "null", and "undefined" strings to {@code null}.
	 * @param data The raw multipart form value.
	 * @return The original value, or {@code null} if the value is empty or represents a null/undefined value.
	 */
	public static String multipartCorrection(final String data) {
		if (data == null) {
			return null;
		}
		if (data.isEmpty()) {
			return null;
		}
		if (data.contentEquals("null")) {
			return null;
		}
		if (data.contentEquals("undefined")) {
			return null;
		}
		return data;
	}

	/**
	 * Detects the MIME type of binary data using Apache Tika.
	 * @param data The binary content to analyze.
	 * @return The detected MIME type string.
	 */
	public static String getMimeType(final byte[] data) {
		final Tika tika = new Tika();
		final String mimeType = tika.detect(data);
		return mimeType;
	}

	/**
	 * Downloads an image from a URI and attaches it as a cover to a database entity.
	 * @param <CLASS_TYPE> The entity class type.
	 * @param <ID_TYPE> The entity identifier type.
	 * @param ioDb The database access instance.
	 * @param clazz The entity class.
	 * @param id The entity identifier.
	 * @param url The URL to download the image from.
	 * @throws Exception If the download fails, the entity is not found, or the MIME type is unsupported.
	 */
	public static <CLASS_TYPE, ID_TYPE> void uploadCoverFromUri(
			final DBAccessMongo ioDb,
			final Class<CLASS_TYPE> clazz,
			final ID_TYPE id,
			final String url) throws Exception {

		LOGGER.info("    - id: {}", id);
		LOGGER.info("    - url: {} ", url);
		final CLASS_TYPE media = ioDb.getById(clazz, id);
		if (media == null) {
			throw new InputException(clazz.getCanonicalName(),
					"[" + id.toString() + "] Id does not exist or removed...");
		}
		// Download data:

		final Client client = ClientBuilder.newClient();
		byte[] dataResponse = null;
		try {
			final WebTarget target = client.target(url);
			final Response response = target.request().get();
			if (response.getStatus() != 200) {
				throw new FailException(Response.Status.BAD_GATEWAY,
						clazz.getCanonicalName() + "[" + id.toString() + "] Can not download the media");
			}
			dataResponse = response.readEntity(byte[].class);
		} catch (final Exception ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR,
					clazz.getCanonicalName() + "[" + id.toString() + "] can not create input media", ex);
		}
		if (dataResponse == null) {
			throw new FailException(Response.Status.NOT_ACCEPTABLE,
					clazz.getCanonicalName() + "[" + id.toString() + "] Data does not exist");
		}
		if (dataResponse.length == 0 || dataResponse.length == 50 * 1024 * 1024) {
			throw new FailException(Response.Status.NOT_ACCEPTABLE, clazz.getCanonicalName() + "[" + id.toString()
					+ "] Data size is not correct " + dataResponse.length);
		}

		final long tmpUID = getTmpDataId();
		final String sha512 = saveTemporaryFile(dataResponse, tmpUID);
		Data data = getWithSha512(ioDb, sha512);
		final String mimeType = getMimeType(dataResponse);
		if (!Arrays.asList(SUPPORTED_IMAGE_MIME_TYPE).contains(mimeType)) {
			throw new FailException(Response.Status.NOT_ACCEPTABLE,
					clazz.getCanonicalName() + "[" + id.toString() + "] Data CoverType is not acceptable: " + mimeType
							+ "support only: " + String.join(", ", SUPPORTED_IMAGE_MIME_TYPE));

		}
		if (data == null) {
			LOGGER.info("Need to add the data in the BDD ... ");
			try {
				data = createNewData(ioDb, tmpUID, url, sha512, mimeType);
			} catch (final IOException ex) {
				removeTemporaryFile(tmpUID);
				throw new FailException(Response.Status.NOT_MODIFIED,
						clazz.getCanonicalName() + "[" + id.toString() + "] can not create input media", ex);
			}
		} else if (data.getDeleted()) {
			LOGGER.error("Data already exist but deleted");
			undelete(ioDb, data.getOid());
			data.setDeleted(false);
		} else {
			LOGGER.error("Data already exist ... all good");
		}
		ListInDbTools.addLink(clazz, id, null, data.getOid());
	}

	/**
	 * Downloads data from a URI and stores it in the database.
	 * @param url The URL to download from.
	 * @return The ObjectId of the stored {@link Data} record.
	 * @throws Exception If the download fails or the MIME type is unsupported.
	 */
	public static ObjectId uploadDataFromUri(final String url) throws Exception {
		// Download data:
		final Client client = ClientBuilder.newClient();
		byte[] dataResponse = null;
		try {
			final WebTarget target = client.target(url);
			final Response response = target.request().get();
			if (response.getStatus() != 200) {
				throw new FailException(Response.Status.BAD_GATEWAY, "Can not download the media");
			}
			dataResponse = response.readEntity(byte[].class);
		} catch (final Exception ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "[] can not create input media", ex);
		}
		if (dataResponse == null) {
			throw new FailException(Response.Status.NOT_ACCEPTABLE, "Data does not exist");
		}
		if (dataResponse.length == 0 || dataResponse.length == 50 * 1024 * 1024) {
			throw new FailException(Response.Status.NOT_ACCEPTABLE, "Data size is not correct " + dataResponse.length);
		}

		final long tmpUID = getTmpDataId();
		final String sha512 = saveTemporaryFile(dataResponse, tmpUID);
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo ioDb = ctx.get();
			Data data = getWithSha512(ioDb, sha512);
			final String mimeType = getMimeType(dataResponse);
			if (!Arrays.asList(SUPPORTED_IMAGE_MIME_TYPE).contains(mimeType)) {
				throw new FailException(Response.Status.NOT_ACCEPTABLE, " Data CoverType is not acceptable: "
						+ mimeType + "support only: " + String.join(", ", SUPPORTED_IMAGE_MIME_TYPE));
			}
			if (data == null) {
				LOGGER.info("Need to add the data in the BDD ... ");
				try {
					data = createNewData(ioDb, tmpUID, url, sha512, mimeType);
				} catch (final IOException ex) {
					removeTemporaryFile(tmpUID);
					throw new FailException(Response.Status.NOT_MODIFIED, "Can not create input media", ex);
				}
			} else if (data.getDeleted()) {
				LOGGER.error("Data already exist but deleted");
				undelete(ioDb, data.getOid());
				data.setDeleted(false);
			} else {
				LOGGER.error("Data already exist ... all good");
			}
			return data.getOid();
		}
	}

	/**
	 * Uploads a file from a multipart form submission and stores it in the database.
	 * @param <CLASS_TYPE> The entity class type.
	 * @param <ID_TYPE> The entity identifier type.
	 * @param fileInputStream The uploaded file input stream.
	 * @param fileMetaData The multipart form metadata (file name, etc.).
	 * @return The ObjectId of the stored {@link Data} record.
	 * @throws Exception If the upload or database insertion fails.
	 */
	public static <CLASS_TYPE, ID_TYPE> ObjectId uploadData(
			final InputStream fileInputStream,
			final FormDataContentDisposition fileMetaData) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo ioDb = ctx.get();
			LOGGER.info("Upload media file: {}", fileMetaData);
			LOGGER.info("    - file_name: {} ", fileMetaData.getFileName());
			LOGGER.info("    - fileInputStream: {}", fileInputStream);
			LOGGER.info("    - fileMetaData: {}", fileMetaData);

			final long tmpUID = getTmpDataId();
			final String sha512 = saveTemporaryFile(fileInputStream, tmpUID);
			Data data = getWithSha512(ioDb, sha512);
			if (data == null) {
				LOGGER.info("Need to add the data in the BDD ... ");
				try {
					data = createNewData(ioDb, tmpUID, fileMetaData.getFileName(), sha512);
				} catch (final IOException ex) {
					removeTemporaryFile(tmpUID);
					throw new FailException(Response.Status.NOT_MODIFIED, "Can not create input media", ex);
				}
			} else if (data.getDeleted()) {
				LOGGER.error("Data already exist but deleted");
				undelete(ioDb, data.getOid());
				data.setDeleted(false);
			} else {
				LOGGER.error("Data already exist ... all good");
			}
			return data.getOid();
		}
	}

	/**
	 * Uploads a cover image from a multipart form submission and attaches it to a database entity.
	 * @param <CLASS_TYPE> The entity class type.
	 * @param <ID_TYPE> The entity identifier type.
	 * @param clazz The entity class.
	 * @param id The entity identifier.
	 * @param fileInputStream The uploaded file input stream.
	 * @param fileMetaData The multipart form metadata.
	 * @throws Exception If the entity is not found or the upload fails.
	 */
	public static <CLASS_TYPE, ID_TYPE> void uploadCover(
			final Class<CLASS_TYPE> clazz,
			final ID_TYPE id,
			final InputStream fileInputStream,
			final FormDataContentDisposition fileMetaData) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccessMongo ioDb = ctx.get();
			// public NodeSmall uploadFile(final FormDataMultiPart form) {
			LOGGER.info("Upload media file: {}", fileMetaData);
			LOGGER.info("    - id: {}", id);
			LOGGER.info("    - file_name: {} ", fileMetaData.getFileName());
			LOGGER.info("    - fileInputStream: {}", fileInputStream);
			LOGGER.info("    - fileMetaData: {}", fileMetaData);
			final CLASS_TYPE media = ioDb.getById(clazz, id);
			if (media == null) {
				throw new InputException(clazz.getCanonicalName(),
						"[" + id.toString() + "] Id does not exist or removed...");
			}

			final long tmpUID = getTmpDataId();
			final String sha512 = saveTemporaryFile(fileInputStream, tmpUID);
			Data data = getWithSha512(ioDb, sha512);
			if (data == null) {
				LOGGER.info("Need to add the data in the BDD ... ");
				try {
					data = createNewData(ioDb, tmpUID, fileMetaData.getFileName(), sha512);
				} catch (final IOException ex) {
					removeTemporaryFile(tmpUID);
					throw new FailException(Response.Status.NOT_MODIFIED,
							clazz.getCanonicalName() + "[" + id.toString() + "] can not create input media", ex);
				}
			} else if (data.getDeleted()) {
				LOGGER.error("Data already exist but deleted");
				undelete(ioDb, data.getOid());
				data.setDeleted(false);
			} else {
				LOGGER.error("Data already exist ... all good");
			}
			ListInDbTools.addLink(clazz, id, "covers", data.getOid());
		}
	}
}
