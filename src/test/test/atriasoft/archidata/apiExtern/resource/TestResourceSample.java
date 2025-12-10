package test.atriasoft.archidata.apiExtern.resource;

import java.util.List;

import org.atriasoft.archidata.annotation.apiGenerator.ApiAsyncType;
import org.atriasoft.archidata.annotation.method.ARCHIVE;
import org.atriasoft.archidata.annotation.method.RESTORE;
import org.atriasoft.archidata.dataAccess.DataAccess;
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
import test.atriasoft.archidata.dataAccess.model.SimpleTable;

@Path("/TestResourceSample")
@Produces({ MediaType.APPLICATION_JSON })
public class TestResourceSample {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestResource.class);

	@GET
	@PermitAll
	public List<SimpleTable> gets() throws Exception {
		return DataAccess.gets(SimpleTable.class);
	}

	@GET
	@Path("{id}")
	@PermitAll
	public SimpleTable get(@PathParam("id") final Long id) throws Exception {
		return DataAccess.getById(SimpleTable.class, id);
	}

	@ARCHIVE
	@Path("{id}")
	@PermitAll
	public SimpleTable archive(@PathParam("id") final Long id) throws Exception {
		return DataAccess.getById(SimpleTable.class, id);
	}

	@RESTORE
	@Path("{id}")
	@PermitAll
	public SimpleTable restore(@PathParam("id") final Long id) throws Exception {
		return DataAccess.getById(SimpleTable.class, id);
	}

	@POST
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public SimpleTable post(final SimpleTable data) throws Exception {
		return DataAccess.insert(data);
	}

	@PATCH
	@Path("{id}")
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public SimpleTable patch(@PathParam("id") final Long id, final SimpleTable data) throws Exception {
		DataAccess.updateById(data, id);
		return DataAccess.getById(SimpleTable.class, id);
	}

	@PUT
	@Path("{id}")
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public SimpleTable put(@PathParam("id") final Long id, final SimpleTable data) throws Exception {
		DataAccess.updateById(data, id);
		return DataAccess.getById(SimpleTable.class, id);
	}

	@DELETE
	@Path("{id}")
	@PermitAll
	public void remove(@PathParam("id") final Long id) throws Exception {
		DataAccess.deleteById(SimpleTable.class, id);
	}

}
