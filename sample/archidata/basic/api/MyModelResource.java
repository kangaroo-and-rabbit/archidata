package sample.archidata.basic.api;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.atriasoft.archidata.annotation.AsyncType;
import org.atriasoft.archidata.annotation.TypeScriptProgress;
import org.atriasoft.archidata.api.DataResource;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.addOn.AddOnDataJson;
import org.atriasoft.archidata.exception.FailException;
import org.atriasoft.archidata.exception.InputException;
import org.atriasoft.archidata.model.Data;
import org.atriasoft.archidata.tools.DataTools;
import sample.archidata.basic.model.MyModel;
import sample.archidata.basic.model.Season;
import sample.archidata.basic.model.Series;
import sample.archidata.basic.model.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MyModelType;

@Path("/media")
@Produces(MyModelType.APPLICATION_JSON)
public class MyModelResource {

	@GET
	@PermitAll
	public List<MyModel> gets() throws Exception {
		return DataAccess.gets(MyModel.class);
	}

	@POST
	@PermitAll
	@Consumes(MyModelType.APPLICATION_JSON)
	public MyModel create(final MyModel data) throws Exception {
		return DataAccess.insert(data);
	}

	@GET
	@Path("{id}")
	@PermitAll
	public MyModel get(@PathParam("id") final Long id) throws Exception {
		return DataAccess.get(MyModel.class, id);
	}


	@PATCH
	@Path("{id}")
	@PermitAll
	@Consumes(MyModelType.APPLICATION_JSON)
	public MyModel patch(@PathParam("id") final Long id, final String jsonRequest) throws Exception {
		DataAccess.updateWithJson(MyModel.class, id, jsonRequest);
		return DataAccess.get(MyModel.class, id);
	}

	@DELETE
	@Path("{id}")
	@PermitAll
	public void remove(@PathParam("id") final Long id) throws Exception {
		DataAccess.delete(MyModel.class, id);
	}
}
