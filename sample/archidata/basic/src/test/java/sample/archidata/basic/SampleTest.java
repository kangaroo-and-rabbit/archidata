package sample.archidata.basic;

import java.lang.reflect.Method;
import java.util.Set;

import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.security.RightAllowed;
import org.atriasoft.archidata.filter.PartRight;
import org.atriasoft.archidata.model.UserByToken;
import org.atriasoft.archidata.tools.JWTWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import sample.archidata.basic.api.TaskResource;
import sample.archidata.basic.filter.SampleAuthenticationFilter;
import sample.archidata.basic.model.Task;

/**
 * Checks that need neither MongoDB nor an HTTP server. The end-to-end behaviour (REST + database)
 * is covered by the archidata test suite, which runs against a real MongoDB.
 */
public class SampleTest {
	private static Validator validator;

	@BeforeAll
	public static void setUp() throws Exception {
		final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
		JWTWrapper.initLocalToken(null);
	}

	/** The AuthenticationFilter answers 403 to an endpoint without access annotation. */
	@Test
	public void everyEndpointDeclaresItsAccess() {
		for (final Method method : TaskResource.class.getDeclaredMethods()) {
			final boolean isEndpoint = method.isAnnotationPresent(GET.class) || method.isAnnotationPresent(POST.class)
					|| method.isAnnotationPresent(PUT.class) || method.isAnnotationPresent(DELETE.class);
			if (!isEndpoint) {
				continue;
			}
			final boolean hasAccess = method.isAnnotationPresent(PermitAll.class)
					|| method.isAnnotationPresent(RolesAllowed.class) || method.isAnnotationPresent(RightAllowed.class);
			Assertions.assertTrue(hasAccess, "Missing access annotation on " + method.getName());
		}
	}

	@Test
	public void validTaskPassesCreateValidation() {
		final Task task = new Task("title", null);
		Assertions.assertTrue(validator.validate(task).isEmpty());
		Assertions.assertTrue(validator.validate(task, GroupCreate.class).isEmpty());
	}

	@Test
	public void missingTitleIsRejected() {
		final Task task = new Task(null, "no title");
		final Set<ConstraintViolation<Task>> violations = validator.validate(task);
		Assertions.assertEquals(1, violations.size());
		Assertions.assertEquals("title", violations.iterator().next().getPropertyPath().toString());
	}

	@Test
	public void clientCannotChooseTheIdOnCreate() {
		final Task task = new Task("title", null);
		task.setId(42L);
		Assertions.assertFalse(validator.validate(task, GroupCreate.class).isEmpty());
	}

	/** The token built by WebLauncherLocal is accepted by the filter and carries the ADMIN role. */
	@Test
	public void localTokenIsAccepted() throws Exception {
		final String token = WebLauncherLocal.createToken("admin", "ADMIN");
		Assertions.assertNotNull(token);
		final UserByToken user = new SampleAuthenticationFilter() {
			public UserByToken check(final String value) throws Exception {
				return validateJwtToken(value);
			}
		}.check(token);
		Assertions.assertNotNull(user);
		Assertions.assertEquals(PartRight.READ_WRITE,
				user.getRoleForKey(SampleAuthenticationFilter.APPLICATION_NAME, "ADMIN"));
	}
}
