package org.kar.archidata.api;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.kar.archidata.filter.GenericContext;
import org.kar.archidata.model.Data;
import org.kar.archidata.sqlWrapper.SqlWrapper;
import org.kar.archidata.annotation.security.PermitTokenInURI;
import org.kar.archidata.annotation.security.RolesAllowed;
import org.kar.archidata.util.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;


// https://stackoverflow.com/questions/35367113/jersey-webservice-scalable-approach-to-download-file-and-reply-to-client
// https://gist.github.com/aitoroses/4f7a2b197b732a6a691d

@Path("/data")
@Produces(MediaType.APPLICATION_JSON)
public class DataResource {
	static final Logger logger = LoggerFactory.getLogger(MediaType.class);
    private final static int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    private final static int CHUNK_SIZE_IN = 50 * 1024 * 1024; // 1MB chunks
    /**
     * Upload some datas
     */
    private static long tmpFolderId = 1;

    private static void createFolder(String path) throws IOException {
        if (!Files.exists(java.nio.file.Path.of(path))) {
            //Log.print("Create folder: " + path);
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
    	logger.info("find sha512 = {}", sha512);
        try {
			return SqlWrapper.getWhere(Data.class, "sha512", "=", sha512);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }

    public static Data getWithId(long id) {
    	logger.info("find id = {}", id);
        try {
			return SqlWrapper.get(Data.class, id);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }

    public static Data createNewData(long tmpUID, String originalFileName, String sha512) throws IOException {
        // determine mime type:
    	Data injectedData = new Data();
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
        injectedData.mimeType = mimeType;
        injectedData.sha512 = sha512;
        String tmpPath = getTmpFileInData(tmpUID);
        injectedData.size = Files.size(Paths.get(tmpPath));

        try {
        	injectedData = SqlWrapper.insert(injectedData);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
        String mediaPath = getFileData(injectedData.id);
        logger.info("src = {}", tmpPath);
        logger.info("dst = {}", mediaPath);
        Files.move(Paths.get(tmpPath), Paths.get(mediaPath), StandardCopyOption.ATOMIC_MOVE);
        logger.info("Move done");
        return injectedData;
    }

    public static String saveTemporaryFile(InputStream uploadedInputStream, long idData) {
        return saveFile(uploadedInputStream, DataResource.getTmpFileInData(idData));
    }

    public static void removeTemporaryFile(long idData) {
        String filepath = DataResource.getTmpFileInData(idData);
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
    static String saveFile(InputStream uploadedInputStream, String serverLocation) {
        String out = "";
        try {
            OutputStream outpuStream = new FileOutputStream(new File(
                    serverLocation));
            int read = 0;
            byte[] bytes = new byte[CHUNK_SIZE_IN];
            MessageDigest md = MessageDigest.getInstance("SHA-512");

            outpuStream = new FileOutputStream(new File(serverLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                //logger.info("write {}", read);
                md.update(bytes, 0, read);
                outpuStream.write(bytes, 0, read);
            }
            logger.info("Flush input stream ... {}", serverLocation);
            System.out.flush();
            outpuStream.flush();
            outpuStream.close();
            // create the end of sha512
            byte[] sha512Digest = md.digest();
            // convert in hexadecimal
            out = bytesToHex(sha512Digest);
            uploadedInputStream.close();
        } catch (IOException ex) {
        	logger.info("Can not write in temporary file ... ");
            ex.printStackTrace();
        } catch (NoSuchAlgorithmException ex) {
        	logger.info("Can not find sha512 algorithms");
            ex.printStackTrace();
        }
        return out;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }


    public Data getSmall(Long id) {
        try {
			return SqlWrapper.get(Data.class, id);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }

    @POST
    @Path("/upload/")
    @Consumes({MediaType.MULTIPART_FORM_DATA})
    @RolesAllowed("ADMIN")
    public Response uploadFile(@Context SecurityContext sc, @FormDataParam("file") InputStream fileInputStream, @FormDataParam("file") FormDataContentDisposition fileMetaData) {
    	GenericContext gc = (GenericContext) sc.getUserPrincipal();
    	logger.info("===================================================");
    	logger.info("== DATA uploadFile {}", (gc==null?"null":gc.userByToken));
    	logger.info("===================================================");
        //public NodeSmall uploadFile(final FormDataMultiPart form) {
    	logger.info("Upload file: ");
        String filePath = ConfigBaseVariable.getTmpDataFolder() + File.separator + tmpFolderId++;
        try {
            createFolder(ConfigBaseVariable.getTmpDataFolder() + File.separator);
        } catch (IOException e) {
            e.printStackTrace();
        }
        saveFile(fileInputStream, filePath);
        return Response.ok("Data uploaded successfully !!").build();
        //return null;
    }

    @GET
    @Path("{id}")
    @PermitTokenInURI
    @RolesAllowed("USER")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response retriveDataId(@Context SecurityContext sc, @QueryParam(HttpHeaders.AUTHORIZATION) String token, @HeaderParam("Range") String range, @PathParam("id") Long id) throws Exception {
    	GenericContext gc = (GenericContext) sc.getUserPrincipal();
        //logger.info("===================================================");
    	logger.info("== DATA retriveDataId ? id={} user={}", id, (gc==null?"null":gc.userByToken));
        //logger.info("===================================================");
        Data value = getSmall(id);
        if (value == null) {
            Response.status(404).
                    entity("media NOT FOUND: " + id).
                    type("text/plain").
                    build();
        }
        return buildStream(ConfigBaseVariable.getMediaDataFolder() + File.separator + id + File.separator + "data", range, value.mimeType);
    }

    @GET
    @Path("thumbnail/{id}")
    @RolesAllowed("USER")
    @PermitTokenInURI
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    //@CacheMaxAge(time = 10, unit = TimeUnit.DAYS)
    public Response retriveDataThumbnailId(@Context SecurityContext sc,
    		@QueryParam(HttpHeaders.AUTHORIZATION) String token,
    		@HeaderParam("Range") String range,
    		@PathParam("id") Long id) throws Exception {
        //GenericContext gc = (GenericContext) sc.getUserPrincipal();
        //logger.info("===================================================");
        //logger.info("== DATA retriveDataThumbnailId ? {}", (gc==null?"null":gc.user));
        //logger.info("===================================================");
        Data value = getSmall(id);
        if (value == null) {
            return Response.status(404).
                    entity("media NOT FOUND: " + id).
                    type("text/plain").
                    build();
        }
        String filePathName = ConfigBaseVariable.getMediaDataFolder() + File.separator + id + File.separator + "data";
        File inputFile = new File(filePathName);
        if (!inputFile.exists()) {
            return Response.status(404).
                    entity("{\"error\":\"media Does not exist: " + id + "\"}").
                    type("application/json").
                    build();
        }
        if (    value.mimeType.contentEquals("image/jpeg")
                || value.mimeType.contentEquals("image/png")
        //        || value.mimeType.contentEquals("image/webp")
        ) {
            // reads input image
            BufferedImage inputImage = ImageIO.read(inputFile);
            int scaledWidth = 250;
            int scaledHeight = (int)((float)inputImage.getHeight() / (float)inputImage.getWidth() * (float) scaledWidth);
            // creates output image
            BufferedImage outputImage = new BufferedImage(scaledWidth,
                                        scaledHeight, inputImage.getType());

            // scales the input image to the output image
            Graphics2D g2d = outputImage.createGraphics();
            g2d.drawImage(inputImage, 0, 0, scaledWidth, scaledHeight, null);
            g2d.dispose();
            // create the output stream:
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
            	// TODO: check how to remove buffer file !!! here, it is not needed at all...
			ImageIO.write( outputImage, "JPG", baos);
			} catch (IOException e) {
				e.printStackTrace();
                return Response.status(500).
                        entity("Internal Error: resize fail: " + e.getMessage()).
                        type("text/plain").
                        build();
			}
            byte[] imageData = baos.toByteArray();
            //Response.ok(new ByteArrayInputStream(imageData)).build();
            Response.ResponseBuilder out = Response.ok(imageData)
                    .header(HttpHeaders.CONTENT_LENGTH, imageData.length);
            out.type("image/jpeg");
            // TODO: move this in a decorator !!!
            CacheControl cc = new CacheControl();
            cc.setMaxAge(3600);
            cc.setNoCache(false);
            out.cacheControl(cc);
            return out.build();
        }
        return buildStream(filePathName, range, value.mimeType);
    }
    //@Secured
    @GET
    @Path("{id}/{name}")
    @PermitTokenInURI
    @RolesAllowed("USER")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response retriveDataFull(@Context SecurityContext sc, @QueryParam(HttpHeaders.AUTHORIZATION) String token, @HeaderParam("Range") String range, @PathParam("id") Long id, @PathParam("name") String name) throws Exception {
        GenericContext gc = (GenericContext) sc.getUserPrincipal();
        //logger.info("===================================================");
        logger.info("== DATA retriveDataFull ? id={} user={}", id, (gc==null?"null":gc.userByToken));
        //logger.info("===================================================");
        Data value = getSmall(id);
        if (value == null) {
            Response.status(404).
                    entity("media NOT FOUND: " + id).
                    type("text/plain").
                    build();
        }
        return buildStream(ConfigBaseVariable.getMediaDataFolder() + File.separator + id + File.separator + "data", range, value.mimeType);
    }

    /**
     * Adapted from http://stackoverflow.com/questions/12768812/video-streaming-to-ipad-does-not-work-with-tapestry5/12829541#12829541
     *
     * @param range range header
     * @return Streaming output
     * @throws Exception IOException if an error occurs in streaming.
     */
    private Response buildStream(final String filename, final String range, String mimeType) throws Exception {
        File file = new File(filename);
        //logger.info("request range : {}", range);
        // range not requested : Firefox does not send range headers
        if (range == null) {
            final StreamingOutput output = new StreamingOutput() {
                @Override
                public void write(OutputStream out) {
                    try (FileInputStream in = new FileInputStream(file)) {
                        byte[] buf = new byte[1024 * 1024];
                        int len;
                        while ((len = in.read(buf)) != -1) {
                            try {
                                out.write(buf, 0, len);
                                out.flush();
                                //logger.info("---- wrote {} bytes file ----", len);
                            } catch (IOException ex) {
                            	logger.info("remote close connection");
                                break;
                            }
                        }
                    } catch (IOException ex) {
                        throw new InternalServerErrorException(ex);
                    }
                }
            };
            Response.ResponseBuilder out = Response.ok(output)
                    .header(HttpHeaders.CONTENT_LENGTH, file.length());
            if (mimeType != null) {
                out.type(mimeType);
            }
            return out.build();

        }

        String[] ranges = range.split("=")[1].split("-");
        final long from = Long.parseLong(ranges[0]);

        //logger.info("request range : {}", ranges.length);
        //Chunk media if the range upper bound is unspecified. Chrome, Opera sends "bytes=0-"
        long to = CHUNK_SIZE + from;
        if (ranges.length == 1) {
            to = file.length() - 1;
        } else {
            if (to >= file.length()) {
                to = (long) (file.length() - 1);
            }
        }
        final String responseRange = String.format("bytes %d-%d/%d", from, to, file.length());
        //logger.info("responseRange: {}", responseRange);
        final RandomAccessFile raf = new RandomAccessFile(file, "r");
        raf.seek(from);

        final long len = to - from + 1;
        final MediaStreamer streamer = new MediaStreamer(len, raf);
        Response.ResponseBuilder out = Response.ok(streamer)
                .status(Response.Status.PARTIAL_CONTENT)
                .header("Accept-Ranges", "bytes")
                .header("Content-Range", responseRange)
                .header(HttpHeaders.CONTENT_LENGTH, streamer.getLenth())
                .header(HttpHeaders.LAST_MODIFIED, new Date(file.lastModified()));
        if (mimeType != null) {
            out.type(mimeType);
        }
        return out.build();
    }

	public static void undelete(Long id) throws Exception {
		SqlWrapper.unsetDelete(Data.class, id);
	}

}
