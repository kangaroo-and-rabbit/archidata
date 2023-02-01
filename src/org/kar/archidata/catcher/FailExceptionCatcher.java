package org.kar.archidata.catcher;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.kar.archidata.exception.FailException;


public class FailExceptionCatcher 
implements ExceptionMapper<FailException> {
	  @Override
	  public Response toResponse(FailException exception) {
		  RestErrorResponse ret = build(exception);
		  System.out.println("Error UUID=" + ret.uuid);
		  // Not display backtrace ==> this may be a normal case ...
		  //exception.printStackTrace();
	      return Response.status(exception.status)
	        .entity(ret)
	        .type(MediaType.APPLICATION_JSON)
	        .build();
	  }

	  private RestErrorResponse build(FailException exception) {
	      return new RestErrorResponse(exception.status, "Request Fail", exception.getMessage());
	  }

}
