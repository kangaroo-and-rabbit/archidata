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
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.apache.tika.Tika;
import org.atriasoft.archidata.api.DataResource;
import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.DBAccess;
import org.atriasoft.archidata.dataAccess.QueryAnd;
import org.atriasoft.archidata.dataAccess.QueryCondition;
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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

public class DataTools {
	private final static Logger LOGGER = LoggerFactory.getLogger(DataTools.class);

	public final static int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
	public final static int CHUNK_SIZE_IN = 50 * 1024 * 1024; // 1MB chunks
	/** Upload some data */
	private static long tmpFolderId = 1;
	public final static String[] SUPPORTED_IMAGE_MIME_TYPE = { "image/jpeg", "image/png", "image/webp" };
	public final static String[] SUPPORTED_AUDIO_MIME_TYPE = { "audio/x-matroska" };
	public final static String[] SUPPORTED_VIDEO_MIME_TYPE = { "video/x-matroska", "video/webm" };

	public static void createFolder(final String path) throws IOException {
		if (!Files.exists(java.nio.file.Path.of(path))) {
			LOGGER.info("Create folder: " + path);
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

	public static String getTmpFolder() {
		final String filePath = ConfigBaseVariable.getTmpDataFolder() + File.separator + tmpFolderId++;
		try {
			createFolder(ConfigBaseVariable.getTmpDataFolder() + File.separator);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return filePath;
	}

	public static Data getWithSha512(final DBAccess ioDb, final String sha512) {
		try {
			return ioDb.getWhere(Data.class, new Condition(new QueryCondition("sha512", "=", sha512)),
					new ReadAllColumn());
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static Data getWithId(final DBAccess ioDb, final long id) {
		try {
			return ioDb.getWhere(Data.class, new Condition(new QueryAnd(
					List.of(new QueryCondition("deleted", "=", false), new QueryCondition("id", "=", id)))));
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static Data createNewData(
			final DBAccess ioDb,
			final long tmpUID,
			final String originalFileName,
			final String sha512,
			final String mimeType) throws IOException, SQLException {

		final String tmpPath = getTmpFileInData(tmpUID);
		final long fileSize = Files.size(Paths.get(tmpPath));
		Data out = new Data();

		try {
			out.sha512 = sha512;
			out.mimeType = mimeType;
			out.size = fileSize;
			out = ioDb.insert(out);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		final String mediaPath = DataResource.getFileData(out.oid);
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

	public static Data createNewData(
			final DBAccess ioDb,
			final long tmpUID,
			final String originalFileName,
			final String sha512) throws IOException, SQLException {
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

	public static void undelete(final DBAccess ioDb, final ObjectId oid) {
		try {
			ioDb.unsetDelete(Data.class, oid);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String saveTemporaryFile(final InputStream uploadedInputStream, final long idData) {
		return saveFile(uploadedInputStream, getTmpFileInData(idData));
	}

	public static String saveTemporaryFile(final byte[] uploadedInputStream, final long idData) {
		return saveFile(uploadedInputStream, getTmpFileInData(idData));
	}

	public static void removeTemporaryFile(final long idData) {
		final String filepath = getTmpFileInData(idData);
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
	public static String saveFile(final InputStream uploadedInputStream, final String serverLocation) {
		String out = "";
		try {
			int read = 0;
			final byte[] bytes = new byte[CHUNK_SIZE_IN];
			final MessageDigest md = MessageDigest.getInstance("SHA-512");
			final OutputStream outpuStream = new FileOutputStream(new File(serverLocation));
			while ((read = uploadedInputStream.read(bytes)) != -1) {
				// logger.debug("write {}", read);
				md.update(bytes, 0, read);
				outpuStream.write(bytes, 0, read);
			}
			LOGGER.info("Flush input stream ... {}", serverLocation);
			outpuStream.flush();
			outpuStream.close();
			// create the end of sha512
			final byte[] sha512Digest = md.digest();
			// convert in hexadecimal
			out = bytesToHex(sha512Digest);
			uploadedInputStream.close();
		} catch (final IOException ex) {
			LOGGER.error("Can not write in temporary file ... ");
			ex.printStackTrace();
		} catch (final NoSuchAlgorithmException ex) {
			LOGGER.error("Can not find sha512 algorithms");
			ex.printStackTrace();
		}
		return out;
	}

	public static String saveFile(final byte[] bytes, final String serverLocation) {
		String out = "";
		try {
			final OutputStream outpuStream = new FileOutputStream(new File(serverLocation));
			final MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update(bytes, 0, bytes.length);
			outpuStream.write(bytes, 0, bytes.length);

			LOGGER.info("Flush input stream ... {}", serverLocation);
			outpuStream.flush();
			outpuStream.close();
			// create the end of sha512
			final byte[] sha512Digest = md.digest();
			// convert in hexadecimal
			out = bytesToHex(sha512Digest);
		} catch (final IOException ex) {
			LOGGER.error("Can not write in temporary file ... ");
			ex.printStackTrace();
		} catch (final NoSuchAlgorithmException ex) {
			LOGGER.error("Can not find sha512 algorithms");
			ex.printStackTrace();
		}
		return out;
	}

	// curl http://localhost:9993/api/users/3
	// @Secured
	/* @GET
	 * @Path("{id}") //@RolesAllowed("GUEST")
	 * @Produces(MediaType.APPLICATION_OCTET_STREAM) public Response retriveData(@HeaderParam("Range") String range, @PathParam("id") Long id) throws Exception { return retriveDataFull(range, id,
	 * "no-name"); } */

	public static String bytesToHex(final byte[] bytes) {
		final StringBuilder sb = new StringBuilder();
		for (final byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

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

	public static String getMimeType(final byte[] data) {
		final Tika tika = new Tika();
		final String mimeType = tika.detect(data);
		return mimeType;
	}

	public static <CLASS_TYPE, ID_TYPE> void uploadCoverFromUri(
			final DBAccess ioDb,
			final Class<CLASS_TYPE> clazz,
			final ID_TYPE id,
			final String url) throws Exception {

		LOGGER.info("    - id: {}", id);
		LOGGER.info("    - url: {} ", url);
		final CLASS_TYPE media = ioDb.get(clazz, id);
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
					clazz.getCanonicalName() + "[" + id.toString() + "] Data CoverType is not accesptable: " + mimeType
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
			} catch (final SQLException ex) {
				removeTemporaryFile(tmpUID);
				throw new FailException(Response.Status.NOT_MODIFIED,
						clazz.getCanonicalName() + "[" + id.toString() + "] Error in SQL insertion", ex);
			}
		} else if (data.deleted) {
			LOGGER.error("Data already exist but deleted");
			undelete(ioDb, data.oid);
			data.deleted = false;
		} else {
			LOGGER.error("Data already exist ... all good");
		}
		ListInDbTools.addLink(clazz, id, null, data.oid);
	}

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
			final DBAccess ioDb = ctx.get();
			Data data = getWithSha512(ioDb, sha512);
			final String mimeType = getMimeType(dataResponse);
			if (!Arrays.asList(SUPPORTED_IMAGE_MIME_TYPE).contains(mimeType)) {
				throw new FailException(Response.Status.NOT_ACCEPTABLE, " Data CoverType is not accesptable: "
						+ mimeType + "support only: " + String.join(", ", SUPPORTED_IMAGE_MIME_TYPE));
			}
			if (data == null) {
				LOGGER.info("Need to add the data in the BDD ... ");
				try {
					data = createNewData(ioDb, tmpUID, url, sha512, mimeType);
				} catch (final IOException ex) {
					removeTemporaryFile(tmpUID);
					throw new FailException(Response.Status.NOT_MODIFIED, "Can not create input media", ex);
				} catch (final SQLException ex) {
					removeTemporaryFile(tmpUID);
					throw new FailException(Response.Status.NOT_MODIFIED, "Error in SQL insertion", ex);
				}
			} else if (data.deleted) {
				LOGGER.error("Data already exist but deleted");
				undelete(ioDb, data.oid);
				data.deleted = false;
			} else {
				LOGGER.error("Data already exist ... all good");
			}
			return data.oid;
		}
	}

	public static <CLASS_TYPE, ID_TYPE> ObjectId uploadData(
			final InputStream fileInputStream,
			final FormDataContentDisposition fileMetaData) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccess ioDb = ctx.get();
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
				} catch (final SQLException ex) {
					removeTemporaryFile(tmpUID);
					throw new FailException(Response.Status.NOT_MODIFIED, "Error in DB insertion", ex);
				}
			} else if (data.deleted) {
				LOGGER.error("Data already exist but deleted");
				undelete(ioDb, data.oid);
				data.deleted = false;
			} else {
				LOGGER.error("Data already exist ... all good");
			}
			return data.oid;
		}
	}

	public static <CLASS_TYPE, ID_TYPE> void uploadCover(
			final Class<CLASS_TYPE> clazz,
			final ID_TYPE id,
			final InputStream fileInputStream,
			final FormDataContentDisposition fileMetaData) throws Exception {
		try (DataAccessConnectionContext ctx = new DataAccessConnectionContext()) {
			final DBAccess ioDb = ctx.get();
			// public NodeSmall uploadFile(final FormDataMultiPart form) {
			LOGGER.info("Upload media file: {}", fileMetaData);
			LOGGER.info("    - id: {}", id);
			LOGGER.info("    - file_name: {} ", fileMetaData.getFileName());
			LOGGER.info("    - fileInputStream: {}", fileInputStream);
			LOGGER.info("    - fileMetaData: {}", fileMetaData);
			final CLASS_TYPE media = ioDb.get(clazz, id);
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
				} catch (final SQLException ex) {
					removeTemporaryFile(tmpUID);
					throw new FailException(Response.Status.NOT_MODIFIED,
							clazz.getCanonicalName() + "[" + id.toString() + "] Error in DB insertion", ex);
				}
			} else if (data.deleted) {
				LOGGER.error("Data already exist but deleted");
				undelete(ioDb, data.oid);
				data.deleted = false;
			} else {
				LOGGER.error("Data already exist ... all good");
			}
			ListInDbTools.addLink(clazz, id, "covers", data.oid);
		}
	}
}
