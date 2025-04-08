package test.atriasoft.archidata.hybernateValidator.resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import test.atriasoft.archidata.apiExtern.resource.TestResource;
import test.atriasoft.archidata.hybernateValidator.model.ValidatorModel;

@Path("/TestResourceValidator")
@Produces({ MediaType.APPLICATION_JSON })
public class TestResourceValidator {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestResource.class);

	@POST
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public void post(final @QueryParam("queryParametersName") @Min(5) Long value, final @Valid ValidatorModel data)
			throws Exception {
		return;
	}

}
