package test.atriasoft.archidata.apiExtern.resource;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import test.atriasoft.archidata.dataAccess.model.DataWithBsonDocument;

@Path("/DocumentResource")
@Produces({ MediaType.APPLICATION_JSON })
public class DocumentResource {

	private static final Logger LOGGER = LoggerFactory.getLogger(DocumentResource.class);

	@POST
	@PermitAll
	@Consumes(MediaType.APPLICATION_JSON)
	public DataWithBsonDocument create(final DataWithBsonDocument data) throws Exception {
		LOGGER.info("create(...)");
		final DBAccessMongo da = DBAccessMongo.createInterface();
		return da.insert(data);
	}

	@GET
	@Path("{oid}")
	@PermitAll
	public DataWithBsonDocument get(@PathParam("oid") final ObjectId oid) throws Exception {
		LOGGER.info("get({})", oid);
		final DBAccessMongo da = DBAccessMongo.createInterface();
		return da.getById(DataWithBsonDocument.class, oid);
	}
}
