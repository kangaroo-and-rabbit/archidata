package test.atriasoft.archidata.apiExtern.resource;

import org.atriasoft.archidata.annotation.filter.DataAccessSingleConnection;
import org.atriasoft.archidata.checker.DataAccessConnectionContext;
import org.atriasoft.archidata.dataAccess.DBAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/DataAccessTestResource")
@Produces({ MediaType.APPLICATION_JSON })
public class DataAccessTestResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestResource.class);

	@GET
	@Path("getAlreadyConnected")
	@PermitAll
	@DataAccessSingleConnection
	public String getAlreadyConnected() throws Exception {
		// Verify if the connection is in the threadLocal
		try {
			DBAccess db = DataAccessConnectionContext.getConnection();
			return "OK";
		} catch (IllegalStateException ex) {
			return "ERROR";
		}
	}

	@GET
	@Path("getNoConnection")
	@PermitAll
	public String getNoConnection() throws Exception {
		// Verify if the connection is in the threadLocal
		try {
			DBAccess db = DataAccessConnectionContext.getConnection();
			return "ERROR";
		} catch (IllegalStateException ex) {
			return "OK";
		}
	}

}
