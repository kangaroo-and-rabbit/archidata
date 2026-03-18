package test.atriasoft.archidata.externalRestApi;

import java.util.List;

import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel.FieldProperty;
import org.atriasoft.archidata.externalRestApi.model.ExampleGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Tests for {@link ExampleGenerator} auto-generation of field examples
 * based on constraints, field names, and types.
 */
public class TestExampleGenerator {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestExampleGenerator.class);

	// -- Test models with various field names and constraints --

	public static class ModelWithNamedFields {
		public String email;
		public String login;
		public String url;
		public String token;
		public Boolean deleted;
		public Boolean active;
		public Integer width;
		public Integer height;
		public String title;
		public String description;
	}

	public static class ModelWithConstraints {
		@Size(min = 3, max = 50)
		public String username;

		@Email
		public String contactEmail;

		@Min(0)
		@Max(100)
		public Integer score;
	}

	public static class ModelWithIdFields {
		public Long id;
		public String parentId;
	}

	public static class ModelWithDateFields {
		public java.util.Date createdAt;
		public java.time.LocalDate birthDate;
		public java.time.LocalTime startTime;
	}

	public static class ModelWithPlainTypes {
		public String someField;
		public Boolean flag;
		public Long number;
		public Double ratio;
	}

	// -- Tests --

	@Test
	public void testFieldNameHeuristics() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ModelWithNamedFields.class);

		final ClassObjectModel model = findModel(api, ModelWithNamedFields.class);
		Assertions.assertNotNull(model);

		assertFieldExample(model, "email", "user@example.com");
		assertFieldExample(model, "login", "example-name");
		assertFieldExample(model, "url", "https://example.com/resource");
		assertFieldExample(model, "token", "abc123xyz789");
		assertFieldExample(model, "deleted", "false");
		assertFieldExample(model, "active", "true");
		assertFieldExample(model, "width", "1920");
		assertFieldExample(model, "height", "1080");
		assertFieldExample(model, "title", "Example Title");
		assertFieldExample(model, "description", "Example description text");
	}

	@Test
	public void testConstraintBasedExamples() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ModelWithConstraints.class);

		final ClassObjectModel model = findModel(api, ModelWithConstraints.class);
		Assertions.assertNotNull(model);

		// @Email → "user@example.com"
		assertFieldExample(model, "contactEmail", "user@example.com");

		// @Min(0) → "0"
		final FieldProperty scoreField = findField(model, "score");
		Assertions.assertNotNull(scoreField);
		Assertions.assertNotNull(scoreField.example());
		Assertions.assertEquals("0", scoreField.example());

		// @Size(min=3, max=50) on "username" → name heuristic applies first, padded to min length
		final FieldProperty usernameField = findField(model, "username");
		Assertions.assertNotNull(usernameField);
		Assertions.assertNotNull(usernameField.example());
		Assertions.assertTrue(usernameField.example().length() >= 3,
				"Example should respect @Size min constraint: " + usernameField.example());
		Assertions.assertTrue(usernameField.example().length() <= 50,
				"Example should respect @Size max constraint: " + usernameField.example());
	}

	@Test
	public void testIdFieldExamples() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ModelWithIdFields.class);

		final ClassObjectModel model = findModel(api, ModelWithIdFields.class);
		Assertions.assertNotNull(model);

		assertFieldExample(model, "id", "123456");
		assertFieldExample(model, "parentId", "123456");
	}

	@Test
	public void testDateFieldExamples() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ModelWithDateFields.class);

		final ClassObjectModel model = findModel(api, ModelWithDateFields.class);
		Assertions.assertNotNull(model);

		assertFieldExample(model, "createdAt", "2000-01-23T01:23:45.678Z");
		assertFieldExample(model, "birthDate", "2000-01-23");
		assertFieldExample(model, "startTime", "01:23:45");
	}

	@Test
	public void testTypeFallbackExamples() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addModel(ModelWithPlainTypes.class);

		final ClassObjectModel model = findModel(api, ModelWithPlainTypes.class);
		Assertions.assertNotNull(model);

		// "someField" has no name heuristic match → falls back to type default "string"
		assertFieldExample(model, "someField", "string");
		assertFieldExample(model, "flag", "false");
		assertFieldExample(model, "number", "0");
		assertFieldExample(model, "ratio", "0.0");
	}

	// -- Helpers --

	private static ClassObjectModel findModel(final AnalyzeApi api, final Class<?> clazz) {
		for (final var m : api.getAllModel()) {
			if (m instanceof final ClassObjectModel com && com.getOriginClasses() == clazz) {
				return com;
			}
		}
		return null;
	}

	private static FieldProperty findField(final ClassObjectModel model, final String name) {
		for (final FieldProperty fp : model.getFields()) {
			if (name.equals(fp.name())) {
				return fp;
			}
		}
		return null;
	}

	private static void assertFieldExample(final ClassObjectModel model, final String fieldName, final String expectedExample) {
		final FieldProperty field = findField(model, fieldName);
		Assertions.assertNotNull(field, "Field '" + fieldName + "' should exist");
		Assertions.assertNotNull(field.example(), "Field '" + fieldName + "' should have an example");
		Assertions.assertEquals(expectedExample, field.example(),
				"Field '" + fieldName + "' example mismatch");
	}
}
