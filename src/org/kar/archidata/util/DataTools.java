package org.kar.archidata.util;

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

import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.kar.archidata.model.Data;
import org.kar.archidata.sqlWrapper.SqlWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataTools {
	static final Logger logger = LoggerFactory.getLogger(DataTools.class);

    public final static int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    public final static int CHUNK_SIZE_IN = 50 * 1024 * 1024; // 1MB chunks
    /**
     * Upload some data
     */
    private static long tmpFolderId = 1;

    public static void createFolder(String path) throws IOException {
        if (!Files.exists(java.nio.file.Path.of(path))) {
        	logger.info("Create folder: " + path);
            Files.createDirectories(java.nio.file.Path.of(path));
        }
    }

    public static long getTmpDataId() {
        return tmpFolderId++;
    }
    public static String getTmpFileInData(long tmpFolderId) {
        String filePath = ConfigBaseVariable.getTmpDataFolder() + File.separator + tmpFolderId;
        try {
            createFolder(ConfigBaseVariable.getTmpDataFolder() + File.separator);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }
    
    public static String getTmpFolder() {
        String filePath = ConfigBaseVariable.getTmpDataFolder() + File.separator + tmpFolderId++;
        try {
            createFolder(ConfigBaseVariable.getTmpDataFolder() + File.separator);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }

    public static String getFileData(long tmpFolderId) {
        String filePath = ConfigBaseVariable.getMediaDataFolder() + File.separator + tmpFolderId + File.separator + "data";
        try {
            createFolder(ConfigBaseVariable.getMediaDataFolder() + File.separator + tmpFolderId + File.separator);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return filePath;
    }

    public static Data getWithSha512(String sha512) {
    	try {
			return SqlWrapper.getWhere(Data.class, "sha512", "=", sha512);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }

    public static Data getWithId(long id) {
    	try {
			return SqlWrapper.getWhere(Data.class, "deleted", "=", false, "id", "=", id);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return null;
    }

    public static Data createNewData(long tmpUID, String originalFileName, String sha512) throws IOException, SQLException {
        // determine mime type:
        String mimeType = "";
        String extension = originalFileName.substring(originalFileName.lastIndexOf('.') + 1);
        switch (extension.toLowerCase()) {
            case "jpg":
            case "jpeg":
                mimeType = "image/jpeg";
                break;
            case "png":
                mimeType = "image/png";
                break;
            case "webp":
                mimeType = "image/webp";
                break;
            case "mka":
                mimeType = "audio/x-matroska";
                break;
            case "mkv":
                mimeType = "video/x-matroska";
                break;
            case "webm":
                mimeType = "video/webm";
                break;
            default:
                throw new IOException("Can not find the mime type of data input: '" + extension + "'");
        }
        String tmpPath = getTmpFileInData(tmpUID);
        long fileSize = Files.size(Paths.get(tmpPath));
        Data out = new Data();;
        try {
        	out.sha512 = sha512;
        	out.mimeType = mimeType;
        	out.size = fileSize;
        	out = SqlWrapper.insert(out);
        } catch (SQLException ex) {
            ex.printStackTrace();
            return null;
        } catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
            return null;
		}

        String mediaPath = getFileData(out.id);
        logger.info("src = {}", tmpPath);
        logger.info("dst = {}", mediaPath);
        Files.move(Paths.get(tmpPath), Paths.get(mediaPath), StandardCopyOption.ATOMIC_MOVE);

        logger.info("Move done");
        // all is done the file is correctly installed...
        return out;
    }

    public static void undelete(Long id) {
		try {
			SqlWrapper.unsetDelete(Data.class, id);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static String saveTemporaryFile(InputStream uploadedInputStream, long idData) {
        return saveFile(uploadedInputStream, getTmpFileInData(idData));
    }

    public static void removeTemporaryFile(long idData) {
        String filepath = getTmpFileInData(idData);
        if (Files.exists(Paths.get(filepath))) {
            try {
                Files.delete(Paths.get(filepath));
            } catch (IOException e) {
            	logger.info("can not delete temporary file : {}", Paths.get(filepath));
                e.printStackTrace();
            }
        }
    }

    // save uploaded file to a defined location on the server
    public static String saveFile(InputStream uploadedInputStream, String serverLocation) {
        String out = "";
        try {
            OutputStream outpuStream = new FileOutputStream(new File(
                    serverLocation));
            int read = 0;
            byte[] bytes = new byte[CHUNK_SIZE_IN];
            MessageDigest md = MessageDigest.getInstance("SHA-512");

            outpuStream = new FileOutputStream(new File(serverLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                //logger.debug("write {}", read);
                md.update(bytes, 0, read);
                outpuStream.write(bytes, 0, read);
            }
            logger.info("Flush input stream ... {}", serverLocation);
            outpuStream.flush();
            outpuStream.close();
            // create the end of sha512
            byte[] sha512Digest = md.digest();
            // convert in hexadecimal
            out = bytesToHex(sha512Digest);
            uploadedInputStream.close();
        } catch (IOException ex) {
        	logger.error("Can not write in temporary file ... ");
            ex.printStackTrace();
        } catch (NoSuchAlgorithmException ex) {
        	logger.error("Can not find sha512 algorithms");
            ex.printStackTrace();
        }
        return out;
    }

    // curl http://localhost:9993/api/users/3
    //@Secured
    /*
    @GET
    @Path("{id}")
    //@RolesAllowed("GUEST")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response retriveData(@HeaderParam("Range") String range, @PathParam("id") Long id) throws Exception {
        return retriveDataFull(range, id, "no-name");
    }
    */

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

	public static String multipartCorrection(String data) {
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
    
    public static <T> Response uploadCover(Class<T> clazz,
    		Long id,
            String fileName,
            InputStream fileInputStream,
            FormDataContentDisposition fileMetaData
    ) {
        try {
            // correct input string stream :
            fileName = multipartCorrection(fileName);

            //public NodeSmall uploadFile(final FormDataMultiPart form) {
            logger.info("Upload media file: {}", fileMetaData);
            logger.info("    - id: {}", id);
            logger.info("    - file_name: ", fileName);
            logger.info("    - fileInputStream: {}", fileInputStream);
            logger.info("    - fileMetaData: {}", fileMetaData);
            T media = SqlWrapper.get(clazz, id);
            if (media == null) {
                return Response.notModified("Media Id does not exist or removed...").build();
            }

            long tmpUID = getTmpDataId();
            String sha512 = saveTemporaryFile(fileInputStream, tmpUID);
            Data data = getWithSha512(sha512);
            if (data == null) {
                logger.info("Need to add the data in the BDD ... ");
                try {
                    data = createNewData(tmpUID, fileName, sha512);
                } catch (IOException ex) {
                    removeTemporaryFile(tmpUID);
                    ex.printStackTrace();
                    return Response.notModified("can not create input media").build();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    removeTemporaryFile(tmpUID);
                    return Response.notModified("Error in SQL insertion ...").build();
                }
            } else if (data.deleted == true) {
            	logger.error("Data already exist but deleted");
                undelete(data.id);
                data.deleted = false;
            } else {
                logger.error("Data already exist ... all good");
            }
            // Fist step: retrieve all the Id of each parents:...
            logger.info("Find typeNode");
            SqlWrapper.addLink(clazz, id, "cover", data.id);
            return Response.ok(SqlWrapper.get(clazz, id)).build();
        } catch (Exception ex) {
            System.out.println("Cat ann unexpected error ... ");
            ex.printStackTrace();
        }
        return Response.serverError().build();
    }
}
