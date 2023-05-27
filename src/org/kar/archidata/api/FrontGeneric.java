package org.kar.archidata.api;

import java.io.File;
import java.util.List;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

import org.kar.archidata.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FrontGeneric {
	static final Logger logger = LoggerFactory.getLogger(FrontGeneric.class);
	
	protected String baseFrontFolder = "/data/front";
	
	private String getExtension(String filename) {
    	if (filename.contains(".")) {
    		return filename.substring(filename.lastIndexOf(".") + 1);
    	}
    	return "";
	}
    private Response retrive(String fileName) throws Exception {
        String filePathName = baseFrontFolder + File.separator + fileName;
    	String extention = getExtension(filePathName);
    	String mineType = null;
    	logger.debug("try retrive : '{}' '{}'", filePathName, extention);
    	if (extention.length() !=0 && extention.length() <= 5) {
	    	if (extention.equalsIgnoreCase("jpg") || extention.equalsIgnoreCase("jpeg")) {
	    		mineType = "image/jpeg";
	    	} else if (extention.equalsIgnoreCase("gif")) {
	    		mineType = "image/gif";
	    	} else if (extention.equalsIgnoreCase("png")) {
	    		mineType = "image/png";
	    	} else if (extention.equalsIgnoreCase("svg")) {
	    		mineType = "image/svg+xml";
	    	} else if (extention.equalsIgnoreCase("webp")) {
	    		mineType = "image/webp";
	    	} else if (extention.equalsIgnoreCase("js")) {
	    		mineType = "application/javascript";
	    	} else if (extention.equalsIgnoreCase("json")) {
	    		mineType = "application/json";
	    	} else if (extention.equalsIgnoreCase("ico")) {
	    		mineType = "image/x-icon";
	    	} else if (extention.equalsIgnoreCase("html")) {
	    		mineType = "text/html";
	    	} else if (extention.equalsIgnoreCase("css")) {
	    		mineType = "text/css";
	    	} else {
	    		throw new NotSupportedException("Not supported model: '" + fileName + "'");
	    	}
    	} else {
    		mineType = "text/html";
    		filePathName = baseFrontFolder + File.separator + "index.html";
    	}
    	logger.debug("    ==> '[}'", filePathName);
    	// reads input image
        File download = new File(filePathName);
        if (!download.exists()) {
    		throw new NotFoundException("Not Found: '" + fileName + "' extension='" + extention + "'");
        }
        ResponseBuilder response = Response.ok((Object)download);
        // use this if I want to download the file:
        //response.header("Content-Disposition", "attachment; filename=" + fileName);
        CacheControl cc = new CacheControl();
        cc.setMaxAge(60);
        cc.setNoCache(false);
        response.cacheControl(cc);
        response.type(mineType);
        
        return response.build();
    }

    @GET
    @PermitAll()
    //@Produces(MediaType.APPLICATION_OCTET_STREAM)
    //@CacheMaxAge(time = 1, unit = TimeUnit.DAYS)
    public Response retrive0() throws Exception {
    	return retrive("index.html");
    }
    
    @GET
    @Path("{any: .*}")
    @PermitAll()
    //@Produces(MediaType.APPLICATION_OCTET_STREAM)
    //@CacheMaxAge(time = 10, unit = TimeUnit.DAYS)
    public Response retrive1(@PathParam("any") List<PathSegment> segments) throws Exception {
    	String filename = "";
    	for (PathSegment elem: segments) {
    		if (!filename.isEmpty()) {
    			filename += File.separator;
    		}
    		filename += elem.getPath();
    	}
    	return retrive(filename);
    }
}
