package sample.archidata.basic.api;

import java.util.List;

import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.atriasoft.archidata.annotation.checker.ValidGroup;
import org.atriasoft.archidata.dataAccess.DataAccess;
import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import sample.archidata.basic.model.Task;

/**
 * CRUD on {@link Task}, served under {@code <api address>/task}.
 *
 * <p>Every method MUST carry {@code @PermitAll}, {@code @RolesAllowed} or {@code @RightAllowed}:
 * the archidata {@code AuthenticationFilter} answers 403 to a method that declares none of them.
 * A role is checked inside the application group of the token: {@code @RolesAllowed("ADMIN")}
 * requires the claim {@code roles.sample.ADMIN} (see {@code SampleAuthenticationFilter}).
 */
@Path("/task")
@Produces(MediaType.APPLICATION_JSON)
public class TaskResource {
	private static final Logger LOGGER = LoggerFactory.getLogger(TaskResource.class);

	@GET
	@Path("count")
	@PermitAll
	@Operation(description = "Number of tasks still open (public endpoint, no token needed)")
	public long countOpen() throws Exception {
		return DataAccess.count(Task.class, new Condition(Filters.eq("done", false)));
	}

	@GET
	@RolesAllowed("USER")
	@Operation(description = "List all the tasks (the soft-deleted ones are not returned)")
	public List<@NotNull Task> gets() throws Exception {
		return DataAccess.gets(Task.class);
	}

	@GET
	@Path("{id}")
	@RolesAllowed("USER")
	@Operation(description = "Get one task")
	public Task get(@PathParam("id") final Long id) throws Exception {
		final Task task = DataAccess.getById(Task.class, id);
		if (task == null) {
			// Turned into a JSON error body with the HTTP 404 status by the registered catchers.
			throw new NotFoundException("Task not found: " + id);
		}
		return task;
	}

	@POST
	@RolesAllowed("ADMIN")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(description = "Create a task")
	// @Valid: Default-group constraints of the model (@NotNull, @Size), checked by Jersey.
	// @ValidGroup(GroupCreate.class): the create-only constraints, e.g. "id must be absent",
	// checked by the ValidGroupInterceptor registered by ContextGenericTools.addJsr310().
	public Task post(@Valid @ValidGroup(GroupCreate.class) final Task task) throws Exception {
		final Task inserted = DataAccess.insert(task);
		LOGGER.info("Created {}", inserted);
		return inserted;
	}

	@PUT
	@Path("{id}")
	@RolesAllowed("ADMIN")
	@Consumes(MediaType.APPLICATION_JSON)
	@Operation(description = "Replace the editable fields of a task")
	public Task put(@PathParam("id") final Long id, @Valid @ValidGroup(GroupUpdate.class) final Task task)
			throws Exception {
		// Without a FilterValue option, updateById writes every editable field (nulls included).
		final long updated = DataAccess.updateById(task, id);
		if (updated == 0) {
			throw new NotFoundException("Task not found: " + id);
		}
		return DataAccess.getById(Task.class, id);
	}

	@DELETE
	@Path("{id}")
	@RolesAllowed("ADMIN")
	@Operation(description = "Delete a task (soft delete: the document stays with deleted=true)")
	public void remove(@PathParam("id") final Long id) throws Exception {
		DataAccess.deleteById(Task.class, id);
	}
}
