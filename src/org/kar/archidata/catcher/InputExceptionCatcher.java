package org.kar.archidata.catcher;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.kar.archidata.exception.InputException;


public class InputExceptionCatcher 
implements ExceptionMapper<InputException> {
	  @Override
	  public Response toResponse(InputException exception) {
		  RestErrorResponse ret = build(exception);
		  System.out.println("Error UUID=" + ret.uuid);
		  exception.printStackTrace();
	      return Response.status(exception.status)
	        .entity(ret)
	        .type(MediaType.APPLICATION_JSON)
	        .build();
	  }

	  private RestErrorResponse build(InputException exception) {
	      return new RestErrorResponse(exception.status, "Error on input='" + exception.missingVariable + "'" , exception.getMessage());
	  }

}
