package org.kar.archidata.catcher;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.kar.archidata.exception.InputException;
import org.kar.archidata.exception.SystemException;


public class SystemExceptionCatcher 
implements ExceptionMapper<SystemException> {
	  @Override
	  public Response toResponse(SystemException exception) {
		  RestErrorResponse ret = build(exception);
		  System.out.println("Error UUID=" + ret.uuid);
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
