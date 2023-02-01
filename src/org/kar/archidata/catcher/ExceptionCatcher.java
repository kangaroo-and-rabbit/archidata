package org.kar.archidata.catcher;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;


public class ExceptionCatcher 
implements ExceptionMapper<Exception> {
	  @Override
	  public Response toResponse(Exception exception) {
		  System.out.println("Catch exception (not managed...):");
		  RestErrorResponse ret = build(exception);
		  System.out.println("Error UUID=" + ret.uuid);
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
