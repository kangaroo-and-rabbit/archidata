package org.kar.archidata.catcher;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FailException404API 
implements ExceptionMapper<ClientErrorException> {
	final Logger logger = LoggerFactory.getLogger(FailException404API.class);
	  @Override
	  public Response toResponse(ClientErrorException exception) {
		  RestErrorResponse ret = build(exception);
		  logger.error("Error UUID={}", ret.uuid);
	      return Response.status(exception.getResponse().getStatusInfo().toEnum())
	        .entity(ret)
	        .type(MediaType.APPLICATION_JSON)
	        .build();
	  }

	  private RestErrorResponse build(ClientErrorException exception) {
	      return new RestErrorResponse(exception.getResponse().getStatusInfo().toEnum(), "Catch system exception" , exception.getMessage());
	  }

}
