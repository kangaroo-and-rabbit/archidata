package org.kar.archidata.catcher;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.kar.archidata.exception.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SystemExceptionCatcher 
implements ExceptionMapper<SystemException> {
	final Logger logger = LoggerFactory.getLogger(SystemExceptionCatcher.class);
	  @Override
	  public Response toResponse(SystemException exception) {
		  RestErrorResponse ret = build(exception);
		  logger.error("Error UUID={}", ret.uuid);
		  exception.printStackTrace();
	      return Response.status(exception.status)
	        .entity(ret)
	        .type(MediaType.APPLICATION_JSON)
	        .build();
	  }

	  private RestErrorResponse build(SystemException exception) {
	      return new RestErrorResponse(exception.status, "System error", exception.getMessage());
	  }

}
