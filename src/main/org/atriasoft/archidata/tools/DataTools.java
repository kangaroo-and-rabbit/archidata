package org.atriasoft.archidata.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

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
	/** Former input buffer size for file uploads (50 MB).
	 * @deprecated No longer used: a 50 MiB buffer per upload let a few concurrent uploads fill the heap; the
	 *             copy uses a 64 KiB buffer. */
	@Deprecated
	public static final int CHUNK_SIZE_IN = 50 * 1024 * 1024; // 50MB chunks
	/** Buffer used to copy an uploaded stream to its temporary file. */
	private static final int COPY_BUFFER_SIZE = 64 * 1024;
	/** Default largest upload accepted by {@link #uploadData} and {@link #uploadCover} (1 GiB). */
	public static final long DEFAULT_MAX_UPLOAD_SIZE = 1024L * 1024 * 1024;
	/** Largest upload accepted by {@link #uploadData} and {@link #uploadCover}. */
	private static volatile long maxUploadSize = DEFAULT_MAX_UPLOAD_SIZE;
	/** Source of the temporary file identifiers: unpredictable, shared by {@link DataTools} and {@link DataResource}. */
	private static final SecureRandom TMP_ID_RANDOM = new SecureRandom();
	/** Smallest temporary identifier: keeps clear of the small counters older code may still use. */
	private static final long TMP_ID_BASE = 1L << 40;
	/** Number of random names tried before giving up when reserving a temporary file or folder. */
	private static final int TMP_RESERVE_ATTEMPTS = 16;
	/** SVG MIME type. Accepted for upload: an SVG rendered with &lt;img src&gt; does not run its scripts, and
	 * {@link DataResource} serves it (like every active type, see {@link #isActiveMimeType(String)}) as an
	 * attachment in a sandbox, so opening its URL directly does not run it on the origin of the API either. */
	public static final String MIME_SVG = "image/svg+xml";
	/** Content-Security-Policy sent with every stored data: no script, no plugin, no request, unique origin. */
	public static final String DATA_CONTENT_SECURITY_POLICY = "sandbox; default-src 'none'";
	/** Types that a browser runs (scripts, documents) when the URL is opened directly. */
	private static final Set<String> ACTIVE_MIME_TYPES = Set.of(MIME_SVG, "text/html", "application/xhtml+xml",
			"text/xml", "application/xml", "application/javascript", "text/javascript", "application/ecmascript",
			"text/ecmascript", "application/x-javascript", "text/xsl", "application/xslt+xml");

	/**
	 * Tells whether a content type is active: a browser that opens it directly would run scripts on the origin
	 * serving it (HTML, SVG, XML documents, JavaScript).
	 * @param mimeType The content type, parameters allowed ({@code text/html; charset=utf-8}); may be null.
	 * @return {@code true} for an active type (any {@code +xml} type included).
	 */
	public static boolean isActiveMimeType(final String mimeType) {
		if (mimeType == null) {
			return false;
		}
		final String base = mimeType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
		return ACTIVE_MIME_TYPES.contains(base) || base.endsWith("+xml");
	}

	/**
	 * Adds the headers that keep a stored data from running on the origin of the API: always
	 * {@code X-Content-Type-Options: nosniff} and {@link #DATA_CONTENT_SECURITY_POLICY}, and
	 * {@code Content-Disposition: attachment} for an active type ({@link #isActiveMimeType(String)}).
	 * @param builder The response being built.
	 * @param mimeType The content type the data is served with.
	 * @return The same builder.
	 */
	public static Response.ResponseBuilder addSafeServingHeaders(
			final Response.ResponseBuilder builder,
			final String mimeType) {
		builder.header("X-Content-Type-Options", "nosniff");
		builder.header("Content-Security-Policy", DATA_CONTENT_SECURITY_POLICY);
		if (isActiveMimeType(mimeType)) {
			builder.header("Content-Disposition", "attachment");
		}
		return builder;
	}

	/** Supported image MIME types for upload validation. */
	public static final String[] SUPPORTED_IMAGE_MIME_TYPE = { "image/jpeg", "image/png", "image/webp", MIME_SVG };
	/** Supported audio MIME types for upload validation. */
	public static final String[] SUPPORTED_AUDIO_MIME_TYPE = { "audio/x-matroska" };
	/** Supported video MIME types for upload validation. */
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

	private static long randomTmpId() {
		return TMP_ID_BASE + (TMP_ID_RANDOM.nextLong() & ((1L << 62) - 1));
	}

	/**
	 * Returns a new unique temporary data identifier and reserves its file.
	 * <p>
	 * The identifier is random (unpredictable) and its file, {@link #getTmpFileInData(long)}, is created
	 * atomically in the temporary folder ({@code DATA_TMP_FOLDER}): two callers, in this class or in
	 * {@link DataResource}, never get the same file. The file is empty until saved; remove it with
	 * {@link #removeTemporaryFile(long)} when it is not moved to the media folder.
	 * </p>
	 * @return A unique long identifier for temporary file storage.
	 * @throws UncheckedIOException If the file cannot be created (an identifier is never returned without its
	 *                              file), or if 16 random identifiers are all taken.
	 */
	public static long getTmpDataId() {
		for (int attempt = 0; attempt < TMP_RESERVE_ATTEMPTS; attempt++) {
			final long id = randomTmpId();
			try {
				Files.createFile(Paths.get(getTmpFileInData(id)));
				return id;
			} catch (final FileAlreadyExistsException ex) {
				LOGGER.debug("Temporary id already used, draw another one");
			} catch (final IOException ex) {
				throw new UncheckedIOException("Can not reserve the temporary file", ex);
			}
		}
		throw new UncheckedIOException(new IOException(
				"Can not reserve a temporary file: " + TMP_RESERVE_ATTEMPTS + " identifiers already used"));
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
	 * Returns a unique (random) temporary folder and reserves it: the folder is created atomically (with
	 * {@link Files#createDirectory}), so two callers never get the same one. The caller removes it when done.
	 * @return The absolute path to the new, empty temporary folder.
	 * @throws UncheckedIOException If the folder cannot be created, or if 16 random
	 *                              names are all taken.
	 */
	public static String getTmpFolder() {
		try {
			createFolder(ConfigBaseVariable.getTmpDataFolder() + File.separator);
		} catch (final IOException ex) {
			throw new UncheckedIOException("Can not create the temporary data folder", ex);
		}
		for (int attempt = 0; attempt < TMP_RESERVE_ATTEMPTS; attempt++) {
			final String filePath = ConfigBaseVariable.getTmpDataFolder() + File.separator + randomTmpId();
			try {
				Files.createDirectory(Paths.get(filePath));
				return filePath;
			} catch (final FileAlreadyExistsException ex) {
				LOGGER.debug("Temporary folder already used, draw another one");
			} catch (final IOException ex) {
				throw new UncheckedIOException("Can not reserve the temporary folder", ex);
			}
		}
		throw new UncheckedIOException(
				new IOException("Can not reserve a temporary folder: " + TMP_RESERVE_ATTEMPTS + " names already used"));
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
			case "svg" -> MIME_SVG;
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
	 * Returns the largest upload accepted by {@link #uploadData} and {@link #uploadCover}.
	 * @return The limit in bytes.
	 */
	public static long getMaxUploadSize() {
		return maxUploadSize;
	}

	/**
	 * Sets the largest upload accepted by {@link #uploadData} and {@link #uploadCover} (and thus by
	 * {@code POST /data/upload}). A larger upload is refused with HTTP 413 and its temporary file removed.
	 * @param maxSize The limit in bytes, strictly positive.
	 * @throws IllegalArgumentException If the limit is not strictly positive.
	 */
	public static void setMaxUploadSize(final long maxSize) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("The upload size limit must be positive: " + maxSize);
		}
		maxUploadSize = maxSize;
	}

	/**
	 * Saves an input stream to a temporary file, up to a size limit, and returns its SHA-512 hash.
	 * @param uploadedInputStream The input stream to save.
	 * @param idData The temporary file identifier.
	 * @param maxSize The largest size accepted, in bytes.
	 * @return The SHA-512 hex string of the saved data.
	 * @throws FailException HTTP 413 when the stream is larger than {@code maxSize} (the file is removed), HTTP
	 *                       500 when the file cannot be written.
	 */
	public static String saveTemporaryFile(final InputStream uploadedInputStream, final long idData, final long maxSize)
			throws FailException {
		return saveFile(uploadedInputStream, getTmpFileInData(idData), maxSize);
	}

	/**
	 * Saves an input stream to a file, up to a size limit, and computes its SHA-512 hash.
	 * @param uploadedInputStream The input stream to save (closed at the end).
	 * @param serverLocation The destination file path.
	 * @param maxSize The largest size accepted, in bytes.
	 * @return The SHA-512 hex string of the saved data.
	 * @throws FailException HTTP 413 when the stream is larger than {@code maxSize} (the file is removed), HTTP
	 *                       500 when the file cannot be written.
	 */
	public static String saveFile(
			final InputStream uploadedInputStream,
			final String serverLocation,
			final long maxSize) throws FailException {
		final MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-512");
		} catch (final NoSuchAlgorithmException ex) {
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Can not find sha512 algorithms", ex);
		}
		boolean tooLarge = false;
		try (InputStream input = uploadedInputStream;
				OutputStream outputStream = new FileOutputStream(new File(serverLocation))) {
			final byte[] bytes = new byte[COPY_BUFFER_SIZE];
			long total = 0;
			int read;
			while ((read = input.read(bytes)) != -1) {
				total += read;
				if (total > maxSize) {
					tooLarge = true;
					break;
				}
				md.update(bytes, 0, read);
				outputStream.write(bytes, 0, read);
			}
		} catch (final IOException ex) {
			deleteQuietly(serverLocation);
			throw new FailException(Response.Status.INTERNAL_SERVER_ERROR, "Can not write in temporary file", ex);
		}
		if (tooLarge) {
			deleteQuietly(serverLocation);
			throw new FailException(Response.Status.REQUEST_ENTITY_TOO_LARGE,
					"The uploaded data is too large (limit: " + maxSize + " bytes)");
		}
		return bytesToHex(md.digest());
	}

	private static void deleteQuietly(final String path) {
		try {
			Files.deleteIfExists(Paths.get(path));
		} catch (final IOException ex) {
			LOGGER.error("Can not delete temporary file '{}': {}", path, ex.getMessage());
		}
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
			final byte[] bytes = new byte[COPY_BUFFER_SIZE];
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
	 * Detects the MIME type of a file by reading its first bytes using Apache Tika.
	 * This is suitable for large files as it only reads the header.
	 * @param filePath The path to the file to analyze.
	 * @return The detected MIME type string.
	 * @throws IOException If the file cannot be read.
	 */
	public static String getMimeTypeFromFile(final String filePath) throws IOException {
		final Tika tika = new Tika();
		try (final InputStream is = new BufferedInputStream(new FileInputStream(filePath))) {
			return tika.detect(is);
		}
	}

	/**
	 * Detects the type of a file from its content (Tika) and checks it against a list.
	 * @param filePath The file to analyze.
	 * @param accepted The accepted MIME types.
	 * @return The detected type, one of {@code accepted}.
	 * @throws FailException HTTP 406 when the detected type is not accepted.
	 * @throws IOException If the file cannot be read.
	 */
	public static String detectAcceptedMimeType(final String filePath, final String[] accepted)
			throws FailException, IOException {
		final String detectedMimeType = getMimeTypeFromFile(filePath);
		if (!Arrays.asList(accepted).contains(detectedMimeType)) {
			throw new FailException(Response.Status.NOT_ACCEPTABLE, "File content type is not acceptable: "
					+ detectedMimeType + " - supported: " + String.join(", ", accepted));
		}
		return detectedMimeType;
	}

	/** All MIME types accepted for generic file upload. */
	public static final String[] SUPPORTED_UPLOAD_MIME_TYPES = { "image/jpeg", "image/png", "image/webp", MIME_SVG,
			"audio/x-matroska", "video/x-matroska", "video/webm" };

	/** Default largest body downloaded from a remote URL (50 MiB). */
	public static final long DEFAULT_MAX_REMOTE_DATA_SIZE = 50L * 1024 * 1024;
	/** Largest body downloaded by {@link #uploadDataFromUri(String)}, {@link #uploadCoverFromUri} and the proxy. */
	private static volatile long maxRemoteDataSize = DEFAULT_MAX_REMOTE_DATA_SIZE;
	/** Client used to download the remote data: pinned validated address, no automatic redirection, timeouts. */
	private static final SafeHttpFetcher REMOTE_FETCHER = new SafeHttpFetcher();

	/**
	 * Returns the largest body accepted from a remote URL ({@link #uploadDataFromUri(String)},
	 * {@link #uploadCoverFromUri}, {@code ProxyResource}).
	 * @return The limit in bytes.
	 */
	public static long getMaxRemoteDataSize() {
		return maxRemoteDataSize;
	}

	/**
	 * Sets the largest body accepted from a remote URL. A larger answer is refused with HTTP 413 before it is
	 * read whole (its {@code Content-Length} is checked first).
	 * @param maxSize The limit in bytes, strictly positive.
	 * @throws IllegalArgumentException If the limit is not strictly positive.
	 */
	public static void setMaxRemoteDataSize(final long maxSize) {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("The remote data size limit must be positive: " + maxSize);
		}
		maxRemoteDataSize = maxSize;
	}

	/**
	 * Downloads a remote data with the SSRF-safe client.
	 * @param url The URL given by the caller.
	 * @return The non-empty body of the answer.
	 * @throws InputException If the URL or a redirection targets a forbidden address.
	 * @throws FailException If the remote answer is not a 200, is empty, too large, or cannot be read.
	 */
	private static byte[] downloadRemoteData(final String url) throws InputException, FailException {
		final SafeHttpFetcher.Result response;
		try {
			response = REMOTE_FETCHER.get(url, maxRemoteDataSize);
		} catch (final IOException ex) {
			throw new FailException(Response.Status.BAD_GATEWAY, "Can not download the media", ex);
		}
		if (response.status() != 200) {
			throw new FailException(Response.Status.BAD_GATEWAY, "Can not download the media");
		}
		if (response.body().length == 0) {
			throw new FailException(Response.Status.NOT_ACCEPTABLE, "Remote data is empty");
		}
		return response.body();
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
		LOGGER.info("    - url: {} ", LogSanitizer.clean(url));
		final CLASS_TYPE media = ioDb.getById(clazz, id);
		if (media == null) {
			throw new InputException("id", "Id does not exist or removed");
		}
		// Download data:
		final byte[] dataResponse = downloadRemoteData(url);

		final long tmpUID = getTmpDataId();
		// The temporary file is removed whatever happens (refused type, existing data, error): once moved to
		// the media folder it no longer exists there.
		try {
			final String sha512 = saveTemporaryFile(dataResponse, tmpUID);
			Data data = getWithSha512(ioDb, sha512);
			final String mimeType = getMimeType(dataResponse);
			if (!Arrays.asList(SUPPORTED_IMAGE_MIME_TYPE).contains(mimeType)) {
				throw new FailException(Response.Status.NOT_ACCEPTABLE, "Data CoverType is not acceptable: " + mimeType
						+ " - supported: " + String.join(", ", SUPPORTED_IMAGE_MIME_TYPE));

			}
			if (data == null) {
				LOGGER.info("Need to add the data in the BDD ... ");
				try {
					data = createNewData(ioDb, tmpUID, url, sha512, mimeType);
				} catch (final IOException ex) {
					throw new FailException(Response.Status.NOT_MODIFIED, "Can not create input media", ex);
				}
			} else if (data.getDeleted()) {
				LOGGER.error("Data already exist but deleted");
				undelete(ioDb, data.getOid());
				data.setDeleted(false);
			} else {
				LOGGER.error("Data already exist ... all good");
			}
			ListInDbTools.addLink(clazz, id, null, data.getOid());
		} finally {
			removeTemporaryFile(tmpUID);
		}
	}

	/**
	 * Downloads data from a URI and stores it in the database.
	 * @param url The URL to download from.
	 * @return The ObjectId of the stored {@link Data} record.
	 * @throws Exception If the download fails or the MIME type is unsupported.
	 */
	public static ObjectId uploadDataFromUri(final String url) throws Exception {
		// Download data (the URL and every redirection are checked against SSRF):
		final byte[] dataResponse = downloadRemoteData(url);

		final long tmpUID = getTmpDataId();
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final String sha512 = saveTemporaryFile(dataResponse, tmpUID);
			final DBAccessMongo ioDb = ctx.get();
			Data data = getWithSha512(ioDb, sha512);
			final String mimeType = getMimeType(dataResponse);
			if (!Arrays.asList(SUPPORTED_IMAGE_MIME_TYPE).contains(mimeType)) {
				throw new FailException(Response.Status.NOT_ACCEPTABLE, "Data CoverType is not acceptable: " + mimeType
						+ " - supported: " + String.join(", ", SUPPORTED_IMAGE_MIME_TYPE));
			}
			if (data == null) {
				LOGGER.info("Need to add the data in the BDD ... ");
				try {
					data = createNewData(ioDb, tmpUID, url, sha512, mimeType);
				} catch (final IOException ex) {
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
		} finally {
			removeTemporaryFile(tmpUID);
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
			LOGGER.info("Upload media file: {}", LogSanitizer.clean(fileMetaData));
			LOGGER.info("    - file_name: {} ", LogSanitizer.clean(fileMetaData.getFileName()));
			LOGGER.info("    - fileInputStream: {}", fileInputStream);
			LOGGER.info("    - fileMetaData: {}", LogSanitizer.clean(fileMetaData));

			final long tmpUID = getTmpDataId();
			try {
				final String sha512 = saveTemporaryFile(fileInputStream, tmpUID, maxUploadSize);
				// Validate MIME type from actual file content (not just extension)
				detectAcceptedMimeType(getTmpFileInData(tmpUID), SUPPORTED_UPLOAD_MIME_TYPES);
				Data data = getWithSha512(ioDb, sha512);
				if (data == null) {
					LOGGER.info("Need to add the data in the BDD ... ");
					try {
						data = createNewData(ioDb, tmpUID, fileMetaData.getFileName(), sha512);
					} catch (final IOException ex) {
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
			} finally {
				removeTemporaryFile(tmpUID);
			}
		}
	}

	/**
	 * Uploads a cover image from a multipart form submission and attaches it to a database entity.
	 * <p>
	 * The type is detected from the content and must be one of {@link #SUPPORTED_IMAGE_MIME_TYPE}; the stored
	 * type is the detected one, not the one the file name suggests.
	 * </p>
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
			LOGGER.info("Upload media file: {}", LogSanitizer.clean(fileMetaData));
			LOGGER.info("    - id: {}", id);
			LOGGER.info("    - file_name: {} ", LogSanitizer.clean(fileMetaData.getFileName()));
			LOGGER.info("    - fileInputStream: {}", fileInputStream);
			LOGGER.info("    - fileMetaData: {}", LogSanitizer.clean(fileMetaData));
			final CLASS_TYPE media = ioDb.getById(clazz, id);
			if (media == null) {
				throw new InputException("id", "Id does not exist or removed");
			}

			final long tmpUID = getTmpDataId();
			try {
				final String sha512 = saveTemporaryFile(fileInputStream, tmpUID, maxUploadSize);
				// The type comes from the content (Tika), not from the file name given by the client
				final String mimeType = detectAcceptedMimeType(getTmpFileInData(tmpUID), SUPPORTED_IMAGE_MIME_TYPE);
				Data data = getWithSha512(ioDb, sha512);
				if (data == null) {
					LOGGER.info("Need to add the data in the BDD ... ");
					try {
						data = createNewData(ioDb, tmpUID, fileMetaData.getFileName(), sha512, mimeType);
					} catch (final IOException ex) {
						throw new FailException(Response.Status.NOT_MODIFIED, "Can not create input media", ex);
					}
				} else if (data.getDeleted()) {
					LOGGER.error("Data already exist but deleted");
					undelete(ioDb, data.getOid());
					data.setDeleted(false);
				} else {
					LOGGER.error("Data already exist ... all good");
				}
				ListInDbTools.addLink(clazz, id, "covers", data.getOid());
			} finally {
				removeTemporaryFile(tmpUID);
			}
		}
	}
}
