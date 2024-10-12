package test.kar.archidata.apiExtern.resource;

import java.util.List;

import org.kar.archidata.annotation.ARCHIVE;
import org.kar.archidata.annotation.AsyncType;
import org.kar.archidata.annotation.RESTORE;
import org.kar.archidata.dataAccess.DataAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import test.kar.archidata.dataAccess.model.SimpleTable;

@Path("/TestResourceSample")
@Produces({ MediaType.APPLICATION_JSON })
public class TestResourceSample {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestResource.class);
	private final DataAccess da = DataAccess.createInterface();

	@GET
	@PermitAll
	public List<SimpleTable> gets() throws Exception {
		return this.da.gets(SimpleTable.class);
	}

	@GET
	@Path("{id}")
	@PermitAll
	public SimpleTable get(@PathParam("id") final Long id) throws Exception {
		return this.da.get(SimpleTable.class, id);
	}

	@ARCHIVE
	@Path("{id}")
	@PermitAll
	public SimpleTable archive(@PathParam("id") final Long id) throws Exception {
		return this.da.get(SimpleTable.class, id);
	}

	@RESTORE
	@Path("{id}")
	@PermitAll
	public SimpleTable restore(@PathParam("id") final Long id) throws Exception {
		return this.da.get(SimpleTable.class, id);
	}

	@POST
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public SimpleTable post(final SimpleTable data) throws Exception {
		return this.da.insert(data);
	}

	@PATCH
	@Path("{id}")
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public SimpleTable patch(@PathParam("id") final Long id, @AsyncType(SimpleTable.class) final String jsonRequest)
			throws Exception {
		this.da.updateWithJson(SimpleTable.class, id, jsonRequest);
		return this.da.get(SimpleTable.class, id);
	}

	@PUT
	@Path("{id}")
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public SimpleTable put(@PathParam("id") final Long id, final SimpleTable data) throws Exception {
		this.da.update(data, id);
		return this.da.get(SimpleTable.class, id);
	}

	@DELETE
	@Path("{id}")
	@PermitAll
	public void remove(@PathParam("id") final Long id) throws Exception {
		this.da.delete(SimpleTable.class, id);
	}

}
