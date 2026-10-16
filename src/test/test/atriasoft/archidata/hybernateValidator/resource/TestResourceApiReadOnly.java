package test.atriasoft.archidata.hybernateValidator.resource;

import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.options.ForceReadOnlyField;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import test.atriasoft.archidata.dataAccess.model.ApiReadOnlyModel;

/** Endpoints checking that the request body mapper drops the {@code @ApiReadOnly} properties. */
@Path("/TestResourceApiReadOnly")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes(MediaType.APPLICATION_JSON)
public class TestResourceApiReadOnly {

	/** Inserts the body as received, without validation group. */
	@POST
	@Path("/insert")
	@PermitAll
	public ApiReadOnlyModel insert(final ApiReadOnlyModel data) throws Exception {
		// The body object is inserted as is: ForceReadOnlyField shows the protection lies in the JSON input.
		return DataAccess.insert(data, new ForceReadOnlyField());
	}

	/** Returns the body as deserialized, without touching the database. */
	@POST
	@Path("/echo")
	@PermitAll
	public ApiReadOnlyModel echo(final ApiReadOnlyModel data) throws Exception {
		return data;
	}

	/** Reads an entity back. */
	@GET
	@Path("/{id}")
	@PermitAll
	public ApiReadOnlyModel get(@PathParam("id") final Long id) throws Exception {
		return DataAccess.getById(ApiReadOnlyModel.class, id);
	}
}
