package test.kar.archidata.apiExtern.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import test.kar.archidata.apiExtern.model.DataForJSR310;

@Path("/TimeResource")
@Produces({ MediaType.APPLICATION_JSON })
public class TimeResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(TimeResource.class);

	@POST
	@PermitAll
	public DataForJSR310 post(final DataForJSR310 data) throws Exception {
		LOGGER.warn("receive Data: {}", data);
		return data;
	}

}
