package test.kar.archidata.apiExtern.resource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.kar.archidata.annotation.apiGenerator.ApiAsyncType;
import org.kar.archidata.annotation.method.ARCHIVE;
import org.kar.archidata.annotation.method.RESTORE;
import org.kar.archidata.exception.NotFoundException;
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
import test.kar.archidata.apiExtern.model.SimpleArchiveTable;

@Path("/TestResource")
@Produces({ MediaType.APPLICATION_JSON })
public class TestResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestResource.class);
	private static final List<SimpleArchiveTable> data = new ArrayList<>();
	private static long uniqueId = 330;

	@GET
	@PermitAll
	public List<SimpleArchiveTable> gets() throws Exception {
		return TestResource.data;
	}

	@GET
	@Path("{id}")
	@PermitAll
	public SimpleArchiveTable get(@PathParam("id") final Long id) throws Exception {
		LOGGER.info("get({})", id);
		for (final SimpleArchiveTable elem : TestResource.data) {
			if (elem.id.equals(id)) {
				return elem;
			}
		}
		throw new NotFoundException("element does not exist: " + id);
	}

	@ARCHIVE
	@Path("{id}")
	@PermitAll
	public SimpleArchiveTable archive(@PathParam("id") final Long id) throws Exception {
		LOGGER.info("archive({})", id);
		for (final SimpleArchiveTable elem : TestResource.data) {
			if (elem.id.equals(id)) {
				elem.updatedAt = new Date();
				elem.archive = new Date();
				return elem;
			}
		}
		throw new NotFoundException("element does not exist: " + id);
	}

	@RESTORE
	@Path("{id}")
	@PermitAll
	public SimpleArchiveTable restore(@PathParam("id") final Long id) throws Exception {
		LOGGER.info("restore({})", id);
		for (final SimpleArchiveTable elem : TestResource.data) {
			if (elem.id.equals(id)) {
				elem.updatedAt = new Date();
				elem.archive = null;
				return elem;
			}
		}
		throw new NotFoundException("element does not exist: " + id);
	}

	@POST
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public SimpleArchiveTable post(final SimpleArchiveTable data) throws Exception {
		LOGGER.info("post(...)");
		data.id = TestResource.uniqueId;
		TestResource.uniqueId += 5;
		data.updatedAt = new Date();
		data.createdAt = new Date();
		this.data.add(data);
		return data;
	}

	@PATCH
	@Path("{id}")
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public SimpleArchiveTable patch(
			@PathParam("id") final Long id,
			@ApiAsyncType(SimpleArchiveTable.class) final String jsonRequest) throws Exception {
		LOGGER.info("patch({})", id);
		throw new NotFoundException("element does not exist: " + id);
	}

	@PUT
	@Path("{id}")
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public SimpleArchiveTable put(@PathParam("id") final Long id, final SimpleArchiveTable data) throws Exception {
		LOGGER.info("put({})", id);
		throw new NotFoundException("element does not exist: " + id);
	}

	@DELETE
	@Path("{id}")
	@PermitAll
	public void remove(@PathParam("id") final Long id) throws Exception {
		LOGGER.info("remove({})", id);
		TestResource.data.removeIf(e -> e.id.equals(id));
	}

}
