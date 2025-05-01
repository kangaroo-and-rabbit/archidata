package test.atriasoft.archidata.hybernateValidator.resource;

import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import test.atriasoft.archidata.apiExtern.resource.TestResource;
import test.atriasoft.archidata.hybernateValidator.model.ValidatorModelGroup;

@Path("/TestResourceValidGroup")
@Produces({ MediaType.APPLICATION_JSON })
@Consumes(MediaType.APPLICATION_JSON)
public class TestResourceValidGroup {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestResource.class);

	@POST
	@Path("/valid")
	@PermitAll
	public void postValid(final @Valid ValidatorModelGroup data) throws Exception {
		return;
	}

	@POST
	@Path("/create")
	@PermitAll
	public void postValidGroupCreate(final @ValidGroup(groups = { GroupCreate.class }) ValidatorModelGroup data)
			throws Exception {
		return;
	}

	@POST
	@Path("/update")
	@PermitAll
	public void postValidGroupUpdate(final @ValidGroup(groups = { GroupUpdate.class }) ValidatorModelGroup data)
			throws Exception {
		return;
	}

	@POST
	@Path("/update-create")
	@PermitAll
	public void postValidGroupUpdateCreate(
			final @ValidGroup(groups = { GroupUpdate.class, GroupCreate.class }) ValidatorModelGroup data)
			throws Exception {
		return;
	}

	@POST
	@Path("/full")
	@PermitAll
	public void postValidGroupFull(
			final @Valid @ValidGroup(groups = { GroupUpdate.class, GroupCreate.class }) ValidatorModelGroup data)
			throws Exception {
		return;
	}
}
