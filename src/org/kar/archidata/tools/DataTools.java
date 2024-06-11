package org.kar.archidata.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.kar.archidata.api.DataResource;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.QueryAnd;
import org.kar.archidata.dataAccess.QueryCondition;
import org.kar.archidata.dataAccess.addOn.AddOnDataJson;
import org.kar.archidata.dataAccess.options.Condition;
import org.kar.archidata.dataAccess.options.ReadAllColumn;
import org.kar.archidata.exception.FailException;
import org.kar.archidata.exception.InputException;
import org.kar.archidata.model.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;

public class DataTools {
	private final static Logger LOGGER = LoggerFactory.getLogger(DataTools.class);

	public final static int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
	public final static int CHUNK_SIZE_IN = 50 * 1024 * 1024; // 1MB chunks
	/** Upload some data */
	private static long tmpFolderId = 1;

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

	public static Data getWithSha512(final String sha512) {
		try {
			return DataAccess.getWhere(Data.class, new Condition(new QueryCondition("sha512", "=", sha512)),
					new ReadAllColumn());
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static Data getWithId(final long id) {
		try {
			return DataAccess.getWhere(Data.class, new Condition(new QueryAnd(
					List.of(new QueryCondition("deleted", "=", false), new QueryCondition("id", "=", id)))));
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static Data createNewData(final long tmpUID, final String originalFileName, final String sha512)
			throws IOException, SQLException {
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
		final String tmpPath = getTmpFileInData(tmpUID);
		final long fileSize = Files.size(Paths.get(tmpPath));
		Data out = new Data();

		try {
			out.sha512 = sha512;
			out.mimeType = mimeType;
			out.size = fileSize;
			out = DataAccess.insert(out);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		final String mediaPath = DataResource.getFileData(out.uuid);
		LOGGER.info("src = {}", tmpPath);
		LOGGER.info("dst = {}", mediaPath);
		Files.move(Paths.get(tmpPath), Paths.get(mediaPath), StandardCopyOption.ATOMIC_MOVE);

		LOGGER.info("Move done");
		// all is done the file is correctly installed...
		return out;
	}

	public static void undelete(final UUID id) {
		try {
			DataAccess.unsetDelete(Data.class, id);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String saveTemporaryFile(final InputStream uploadedInputStream, final long idData) {
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
			OutputStream outpuStream = new FileOutputStream(new File(serverLocation));
			int read = 0;
			final byte[] bytes = new byte[CHUNK_SIZE_IN];
			final MessageDigest md = MessageDigest.getInstance("SHA-512");

			outpuStream = new FileOutputStream(new File(serverLocation));
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
		return data;
	}

	public static <CLASS_TYPE, ID_TYPE> void uploadCover(
			final Class<CLASS_TYPE> clazz,
			final ID_TYPE id,
			final InputStream fileInputStream,
			final FormDataContentDisposition fileMetaData) throws Exception {
		// public NodeSmall uploadFile(final FormDataMultiPart form) {
		LOGGER.info("Upload media file: {}", fileMetaData);
		LOGGER.info("    - id: {}", id);
		LOGGER.info("    - file_name: ", fileMetaData.getFileName());
		LOGGER.info("    - fileInputStream: {}", fileInputStream);
		LOGGER.info("    - fileMetaData: {}", fileMetaData);
		final CLASS_TYPE media = DataAccess.get(clazz, id);
		if (media == null) {
			throw new InputException(clazz.getCanonicalName(),
					"[" + id.toString() + "] Id does not exist or removed...");
		}

		final long tmpUID = getTmpDataId();
		final String sha512 = saveTemporaryFile(fileInputStream, tmpUID);
		Data data = getWithSha512(sha512);
		if (data == null) {
			LOGGER.info("Need to add the data in the BDD ... ");
			try {
				data = createNewData(tmpUID, fileMetaData.getFileName(), sha512);
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
			undelete(data.uuid);
			data.deleted = false;
		} else {
			LOGGER.error("Data already exist ... all good");
		}
		// Fist step: retrieve all the Id of each parents:...
		LOGGER.info("Find typeNode");
		if (id instanceof final Long idLong) {
			AddOnDataJson.addLink(clazz, idLong, "covers", data.uuid);
		} else if (id instanceof final UUID idUUID) {
			AddOnDataJson.addLink(clazz, idUUID, "covers", data.uuid);
		} else {
			throw new IOException("Fail to add Cover can not detect type...");
		}
	}
}
