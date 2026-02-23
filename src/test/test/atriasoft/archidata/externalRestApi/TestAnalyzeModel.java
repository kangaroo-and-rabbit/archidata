package test.atriasoft.archidata.externalRestApi;

import java.util.List;

import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel;
import org.atriasoft.archidata.externalRestApi.model.ClassObjectModel.FieldProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class TestAnalyzeModel {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestAnalyzeModel.class);

	// -- POJO test model (existing) --

	public class TestObject {
		public Integer value;
	}

	// -- Record test models --

	public record TestRecord(
			Integer value,
			String name) {}

	public record TestAnnotatedRecord(
			@NotNull Integer value,
			@Size(min = 1, max = 100) String name) {}

	// -- Bean test model (private fields + getters/setters) --

	public static class TestBean {
		private Integer value;
		private String name;

		public Integer getValue() {
			return this.value;
		}

		public void setValue(final Integer value) {
			this.value = value;
		}

		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}

	// -- @JsonInclude(NON_NULL) test model --

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public static class TestJsonIncludeModel {
		public Integer value;
		public String name;
	}

	public static class TestNoJsonIncludeModel {
		public Integer value;
		public String name;
	}

	// -- Heritage test models --

	public static class TestParent {
		public String parentField;
	}

	public static class TestChild extends TestParent {
		public Integer childField;
	}

	// -- Tests --

	@Test
	public void testNames() throws Exception {
		final AnalyzeApi apiInterface = new AnalyzeApi();
		apiInterface.addModel(TestObject.class);
		Assertions.assertEquals(2, apiInterface.getAllModel().size());
		final ClassObjectModel model = Assertions.assertInstanceOf(ClassObjectModel.class,
				apiInterface.getAllModel().get(0));

		Assertions.assertEquals("TestObject", model.getName());
		Assertions.assertEquals(false, model.isPrimitive());
		Assertions.assertNull(model.getDescription());
		Assertions.assertNull(model.getExample());
		Assertions.assertNull(model.getExtendsClass());
		Assertions.assertEquals(1, model.getFields().size());
		final FieldProperty fieldProperty = model.getFields().get(0);
		Assertions.assertEquals("value", fieldProperty.name());

		final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class, fieldProperty.model());
		Assertions.assertEquals(Integer.class, classModel.getOriginClasses());

	}

	@Test
	public void testRecordModel() throws Exception {
		final AnalyzeApi apiInterface = new AnalyzeApi();
		apiInterface.addModel(TestRecord.class);

		// Find the TestRecord model (skip primitive/basic types)
		ClassObjectModel model = null;
		for (final var m : apiInterface.getAllModel()) {
			if (m instanceof final ClassObjectModel com && com.getOriginClasses() == TestRecord.class) {
				model = com;
				break;
			}
		}
		Assertions.assertNotNull(model, "TestRecord model should be found");

		Assertions.assertFalse(model.isPrimitive());
		Assertions.assertNull(model.getExtendsClass()); // Records do not have heritage
		Assertions.assertEquals(2, model.getFields().size());

		final List<String> fieldNames = model.getFields().stream().map(FieldProperty::name).toList();
		Assertions.assertTrue(fieldNames.contains("value"), "Should contain 'value' field");
		Assertions.assertTrue(fieldNames.contains("name"), "Should contain 'name' field");

		// Check types
		for (final FieldProperty fp : model.getFields()) {
			if ("value".equals(fp.name())) {
				Assertions.assertEquals(Integer.class, fp.model().getOriginClasses());
			} else if ("name".equals(fp.name())) {
				Assertions.assertEquals(String.class, fp.model().getOriginClasses());
			}
		}
	}

	@Test
	public void testRecordAnnotations() throws Exception {
		final AnalyzeApi apiInterface = new AnalyzeApi();
		apiInterface.addModel(TestAnnotatedRecord.class);

		ClassObjectModel model = null;
		for (final var m : apiInterface.getAllModel()) {
			if (m instanceof final ClassObjectModel com && com.getOriginClasses() == TestAnnotatedRecord.class) {
				model = com;
				break;
			}
		}
		Assertions.assertNotNull(model, "TestAnnotatedRecord model should be found");
		Assertions.assertEquals(2, model.getFields().size());

		for (final FieldProperty fp : model.getFields()) {
			if ("value".equals(fp.name())) {
				Assertions.assertNotNull(fp.annotationNotNull(), "value should have @NotNull");
			} else if ("name".equals(fp.name())) {
				Assertions.assertNotNull(fp.stringSize(), "name should have @Size");
				Assertions.assertEquals(1, fp.stringSize().min());
				Assertions.assertEquals(100, fp.stringSize().max());
			}
		}
	}

	@Test
	public void testBeanModel() throws Exception {
		final AnalyzeApi apiInterface = new AnalyzeApi();
		apiInterface.addModel(TestBean.class);

		ClassObjectModel model = null;
		for (final var m : apiInterface.getAllModel()) {
			if (m instanceof final ClassObjectModel com && com.getOriginClasses() == TestBean.class) {
				model = com;
				break;
			}
		}
		Assertions.assertNotNull(model, "TestBean model should be found");
		Assertions.assertEquals(2, model.getFields().size());

		final List<String> fieldNames = model.getFields().stream().map(FieldProperty::name).toList();
		Assertions.assertTrue(fieldNames.contains("value"), "Should contain 'value' property");
		Assertions.assertTrue(fieldNames.contains("name"), "Should contain 'name' property");

		// Check types
		for (final FieldProperty fp : model.getFields()) {
			if ("value".equals(fp.name())) {
				Assertions.assertEquals(Integer.class, fp.model().getOriginClasses());
			} else if ("name".equals(fp.name())) {
				Assertions.assertEquals(String.class, fp.model().getOriginClasses());
			}
		}
	}

	@Test
	public void testPojoHeritage() throws Exception {
		final AnalyzeApi apiInterface = new AnalyzeApi();
		apiInterface.addModel(TestChild.class);

		ClassObjectModel childModel = null;
		for (final var m : apiInterface.getAllModel()) {
			if (m instanceof final ClassObjectModel com && com.getOriginClasses() == TestChild.class) {
				childModel = com;
				break;
			}
		}
		Assertions.assertNotNull(childModel, "TestChild model should be found");
		// childField only -- parentField should be in the parent model
		Assertions.assertEquals(1, childModel.getFields().size());
		Assertions.assertEquals("childField", childModel.getFields().get(0).name());
		Assertions.assertNotNull(childModel.getExtendsClass(), "Should have parent class reference");
	}

	@Test
	public void testJsonIncludeNonNullDetection() throws Exception {
		// Model WITH @JsonInclude(NON_NULL)
		final AnalyzeApi apiWithJson = new AnalyzeApi();
		apiWithJson.addModel(TestJsonIncludeModel.class);
		ClassObjectModel modelWith = null;
		for (final var m : apiWithJson.getAllModel()) {
			if (m instanceof final ClassObjectModel com && com.getOriginClasses() == TestJsonIncludeModel.class) {
				modelWith = com;
				break;
			}
		}
		Assertions.assertNotNull(modelWith);
		Assertions.assertTrue(modelWith.isJsonIncludeNonNull(),
				"Model with @JsonInclude(NON_NULL) should be detected");

		// Model WITHOUT @JsonInclude(NON_NULL)
		final AnalyzeApi apiWithout = new AnalyzeApi();
		apiWithout.addModel(TestNoJsonIncludeModel.class);
		ClassObjectModel modelWithout = null;
		for (final var m : apiWithout.getAllModel()) {
			if (m instanceof final ClassObjectModel com && com.getOriginClasses() == TestNoJsonIncludeModel.class) {
				modelWithout = com;
				break;
			}
		}
		Assertions.assertNotNull(modelWithout);
		Assertions.assertFalse(modelWithout.isJsonIncludeNonNull(),
				"Model without @JsonInclude should not be detected");
	}

}
