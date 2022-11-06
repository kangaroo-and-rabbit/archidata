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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.SqlWrapper;
import org.kar.archidata.model.Data;
import org.kar.archidata.db.DBEntry;

public class DataTools {

    public final static int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    public final static int CHUNK_SIZE_IN = 50 * 1024 * 1024; // 1MB chunks
    /**
     * Upload some data
     */
    private static long tmpFolderId = 1;

    public static void createFolder(String path) throws IOException {
        if (!Files.exists(java.nio.file.Path.of(path))) {
            System.out.println("Create folder: " + path);
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
        System.out.println("find sha512 = " + sha512);
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        String query = "SELECT `id`, `deleted`, `sha512`, `mime_type`, `size` FROM `data` WHERE `sha512` = ?";
        try {
            PreparedStatement ps = entry.connection.prepareStatement(query);
            ps.setString(1, sha512);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Data out = new Data(rs);
                entry.disconnect();
                return out;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        entry.disconnect();
        return null;

    }

    public static Data getWithId(long id) {
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        String query = "SELECT `id`, `deleted`, `sha512`, `mime_type`, `size` FROM `data` WHERE `deleted` = false AND `id` = ?";
        try {
            PreparedStatement ps = entry.connection.prepareStatement(query);
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Data out = new Data(rs);
                entry.disconnect();
                return out;
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        entry.disconnect();
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
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        long uniqueSQLID = -1;
        try {
            // prepare the request:
            String query = "INSERT INTO `data` (`sha512`, `mime_type`, `size`, `original_name`) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = entry.connection.prepareStatement(query,
                    Statement.RETURN_GENERATED_KEYS);
            int iii = 1;
            ps.setString(iii++, sha512);
            ps.setString(iii++, mimeType);
            ps.setLong(iii++, fileSize);
            ps.setString(iii++, originalFileName);
            // execute the request
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating data failed, no rows affected.");
            }
            // retreive uid inserted
            try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    uniqueSQLID = generatedKeys.getLong(1);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained (1).");
                }
            } catch (Exception ex) {
                System.out.println("Can not get the UID key inserted ... ");
                ex.printStackTrace();
                throw new SQLException("Creating user failed, no ID obtained (2).");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        entry.disconnect();
        System.out.println("Add Data raw done. uid data=" + uniqueSQLID);
        Data out = getWithId(uniqueSQLID);

        String mediaPath = getFileData(out.id);
        System.out.println("src = " + tmpPath);
        System.out.println("dst = " + mediaPath);
        Files.move(Paths.get(tmpPath), Paths.get(mediaPath), StandardCopyOption.ATOMIC_MOVE);

        System.out.println("Move done");
        // all is done the file is corectly installed...

        return out;
    }

    public static void undelete(Long id) {
        DBEntry entry = new DBEntry(GlobalConfiguration.dbConfig);
        String query = "UPDATE `data` SET `deleted` = false WHERE `id` = ?";
        try {
            PreparedStatement ps = entry.connection.prepareStatement(query);
            ps.setLong(1, id);
            ps.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        entry.disconnect();
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
                System.out.println("can not delete temporary file : " + Paths.get(filepath));
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
                //System.out.println("write " + read);
                md.update(bytes, 0, read);
                outpuStream.write(bytes, 0, read);
            }
            System.out.println("Flush input stream ... " + serverLocation);
            System.out.flush();
            outpuStream.flush();
            outpuStream.close();
            // create the end of sha512
            byte[] sha512Digest = md.digest();
            // convert in hexadecimal
            out = bytesToHex(sha512Digest);
            uploadedInputStream.close();
        } catch (IOException ex) {
            System.out.println("Can not write in temporary file ... ");
            ex.printStackTrace();
        } catch (NoSuchAlgorithmException ex) {
            System.out.println("Can not find sha512 algorithms");
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
            System.out.println("Upload media file: " + fileMetaData);
            System.out.println("    - id: " + id);
            System.out.println("    - file_name: " + fileName);
            System.out.println("    - fileInputStream: " + fileInputStream);
            System.out.println("    - fileMetaData: " + fileMetaData);
            System.out.flush();
            T media = SqlWrapper.get(clazz, id);
            if (media == null) {
                return Response.notModified("Media Id does not exist or removed...").build();
            }

            long tmpUID = getTmpDataId();
            String sha512 = saveTemporaryFile(fileInputStream, tmpUID);
            Data data = getWithSha512(sha512);
            if (data == null) {
                System.out.println("Need to add the data in the BDD ... ");
                System.out.flush();
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
                System.out.println("Data already exist but deleted");
                System.out.flush();
                undelete(data.id);
                data.deleted = false;
            } else {
                System.out.println("Data already exist ... all good");
                System.out.flush();
            }
            // Fist step: retrieve all the Id of each parents:...
            System.out.println("Find typeNode");
            SqlWrapper.addLink(clazz, id, "cover", data.id);
            return Response.ok(SqlWrapper.get(clazz, id)).build();
        } catch (Exception ex) {
            System.out.println("Cat ann unexpected error ... ");
            ex.printStackTrace();
        }
        return Response.serverError().build();
    }
}
