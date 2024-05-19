package test.kar.archidata.externalRestApi;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kar.archidata.externalRestApi.AnalyzeModel;
import org.kar.archidata.externalRestApi.model.ClassModel;
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
		final ClassObjectModel model = new ClassObjectModel(TestObject.class);
		final List<ClassModel> models = new ArrayList<>();
		models.add(model);
		AnalyzeModel.fillModel(models);

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
