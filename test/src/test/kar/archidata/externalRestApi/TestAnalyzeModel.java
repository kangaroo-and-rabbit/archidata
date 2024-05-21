package test.kar.archidata.externalRestApi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kar.archidata.externalRestApi.AnalyzeApi;
import org.kar.archidata.externalRestApi.model.ClassObjectModel;
import org.kar.archidata.externalRestApi.model.ClassObjectModel.FieldProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAnalyzeModel {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestAnalyzeModel.class);

	public class TestObject {
		public Integer value;
	}

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

}
