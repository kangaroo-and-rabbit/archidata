package org.kar.archidata.catcher;

import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;


public class ExceptionCatcher 
implements ExceptionMapper<Exception> {
	final Logger logger = LoggerFactory.getLogger(ExceptionCatcher.class);
	  @Override
	  public Response toResponse(Exception exception) {
		  logger.warn("Catch exception (not managed...):");
		  RestErrorResponse ret = build(exception);
		  logger.error("Error UUID={}", ret.uuid);
		  exception.printStackTrace();
	      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
	        .entity(ret)
	        .type(MediaType.APPLICATION_JSON)
	        .build();
	  }

	  private RestErrorResponse build(Exception exception) {
	      return new RestErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "Catch Unknown Exception", exception.getMessage());
	  }

}
