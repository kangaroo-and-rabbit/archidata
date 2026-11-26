package test.atriasoft.archidata.checker;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.atriasoft.archidata.checker.ValidationContext;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import test.atriasoft.archidata.checker.model.ValidatedLists;

/**
 * Characterization of {@link ValidationContext}: the thread bound context, its key/value store
 * and its two ways of running a validator.
 */
public class TestValidationContext {

	public static class Named {
		@NotNull(groups = GroupCreate.class, message = "name required")
		public String name;
		@Null(groups = GroupCreate.class, message = "server only")
		public String serverOnly;
	}

	@AfterEach
	public void closeContext() {
		if (ValidationContext.exists()) {
			ValidationContext.current().close();
		}
	}

	private static Set<String> paths(final Set<? extends ConstraintViolation<?>> violations) {
		final Set<String> out = new TreeSet<>();
		for (final ConstraintViolation<?> violation : violations) {
			out.add(violation.getPropertyPath() + ": " + violation.getMessage());
		}
		return out;
	}

	@Test
	public void testNoContextByDefault() {
		Assertions.assertFalse(ValidationContext.exists());
		Assertions.assertNull(ValidationContext.current());
	}

	@Test
	public void testOpenBindsTheContextToTheThreadAndCloseReleasesIt() {
		final ValidationContext context = ValidationContext.open();
		Assertions.assertTrue(ValidationContext.exists());
		Assertions.assertSame(context, ValidationContext.current());
		context.close();
		Assertions.assertFalse(ValidationContext.exists());
		Assertions.assertNull(ValidationContext.current());
	}

	@Test
	public void testTryWithResourcesReleasesTheContext() {
		try (final ValidationContext context = ValidationContext.open()) {
			Assertions.assertSame(context, ValidationContext.current());
		}
		Assertions.assertFalse(ValidationContext.exists());
	}

	@Test
	public void testAnotherThreadDoesNotSeeTheContext() throws Exception {
		ValidationContext.open();
		final boolean[] seen = new boolean[1];
		final Thread other = new Thread(() -> seen[0] = ValidationContext.exists());
		other.start();
		other.join();
		Assertions.assertFalse(seen[0]);
		Assertions.assertTrue(ValidationContext.exists());
	}

	@Test
	public void testOpenAgainReplacesTheBoundContext() {
		final ValidationContext first = ValidationContext.open();
		final ValidationContext second = ValidationContext.open();
		Assertions.assertNotSame(first, second);
		Assertions.assertSame(second, ValidationContext.current());
		first.close();
	}

	@Test
	public void testKeyValueStore() {
		final ValidationContext context = ValidationContext.open();
		final ObjectId oid = new ObjectId();
		Assertions.assertFalse(context.contains("oid"));
		Assertions.assertNull(context.get("oid"));
		Assertions.assertNull(context.<ObjectId> get("oid", ObjectId.class));
		Assertions.assertSame(context, context.put("oid", oid).put("count", 3L), "put chains");
		Assertions.assertTrue(context.contains("oid"));
		Assertions.assertSame(oid, context.get("oid"));
		Assertions.assertSame(oid, context.<ObjectId> get("oid", ObjectId.class));
		Assertions.assertEquals(3L, context.<Long> get("count", Long.class));
		// a key holding null is present
		context.put("empty", null);
		Assertions.assertTrue(context.contains("empty"));
		Assertions.assertNull(context.<ObjectId> get("empty", ObjectId.class));
		// the last value wins
		context.put("count", 4L);
		Assertions.assertEquals(4L, context.<Long> get("count", Long.class));
	}

	@Test
	public void testTypedGetOfAnotherTypeFails() {
		final ValidationContext context = ValidationContext.open().put("oid", "text");
		final RuntimeException exception = Assertions.assertThrows(RuntimeException.class,
				() -> context.<ObjectId> get("oid", ObjectId.class));
		Assertions.assertEquals("ValidationContext: value for key 'oid' is not of type ObjectId, but String",
				exception.getMessage());
		// a subclass is accepted
		Assertions.assertEquals("text", context.<CharSequence> get("oid", CharSequence.class).toString());
	}

	@Test
	public void testValidateOfNullIsEmpty() {
		final ValidationContext context = ValidationContext.open();
		Assertions.assertTrue(context.validate(null).isEmpty());
		Assertions.assertTrue(context.validate((Named) null, GroupCreate.class).isEmpty());
		// validateThrow of null does not throw either
		context.validateThrow(null);
	}

	@Test
	public void testValidateHonoursTheGroups() {
		final ValidationContext context = ValidationContext.open();
		final Named data = new Named();
		data.serverOnly = "set";
		Assertions.assertEquals(Set.of("name: name required", "serverOnly: server only"),
				paths(context.validate(data, GroupCreate.class)));
		Assertions.assertEquals(Set.of(), paths(context.validate(data, GroupUpdate.class)));
		// no group means the Default group, which none of the constraints belongs to
		Assertions.assertEquals(Set.of(), paths(context.validate(data)));
		data.name = "ok";
		data.serverOnly = null;
		Assertions.assertEquals(Set.of(), paths(context.validate(data, GroupCreate.class)));
	}

	@Test
	public void testValidateThrowCarriesTheViolations() {
		final ValidationContext context = ValidationContext.open();
		final ValidatedLists lists = new ValidatedLists();
		lists.notEmpty = List.of();
		final ConstraintViolationException exception = Assertions.assertThrows(ConstraintViolationException.class,
				() -> context.validateThrow(lists));
		Assertions.assertEquals("Validation failed for " + ValidatedLists.class.getCanonicalName(),
				exception.getMessage());
		Assertions.assertEquals(Set.of("notEmpty: Collection can not be empty"),
				paths(exception.getConstraintViolations()));
		lists.notEmpty = List.of("a");
		context.validateThrow(lists);
	}

	@Test
	public void testValidateThrowWithGroups() {
		final ValidationContext context = ValidationContext.open();
		final Named data = new Named();
		final ConstraintViolationException exception = Assertions.assertThrows(ConstraintViolationException.class,
				() -> context.validateThrow(data, GroupCreate.class));
		Assertions.assertEquals(Set.of("name: name required"), paths(exception.getConstraintViolations()));
		context.validateThrow(data, GroupUpdate.class);
	}
}
