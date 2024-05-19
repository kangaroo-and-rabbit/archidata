package test.kar.archidata.externalRestApi;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kar.archidata.externalRestApi.AnalyzeApi;
import org.kar.archidata.externalRestApi.model.ApiModel;
import org.kar.archidata.externalRestApi.model.ClassEnumModel;
import org.kar.archidata.externalRestApi.model.ClassListModel;
import org.kar.archidata.externalRestApi.model.ClassMapModel;
import org.kar.archidata.externalRestApi.model.ClassObjectModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.GET;

public class TestAnalyzeApiParameterType {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestAnalyzeApiParameterType.class);

	public enum TestEnum {
		PLOP, PLIP
	}

	public class TestObject {
		public int value;
	}

	public class BasicParameter {
		@GET
		public void setInteger1(final int parameter) {}

		@GET
		public void setInteger2(final Integer parameter) {}

		@GET
		public void setString(final String parameter) {}

		@GET
		public void setObject(final TestObject parameter) {}

		@GET
		public void setEnum(final TestEnum parameter) {}
	}

	@Test
	public void testBasicParameter() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(BasicParameter.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(5, api.apiModels.get(0).interfaces.size());
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("setInteger1");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.unnamedElement.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.unnamedElement.get(0));
			Assertions.assertEquals(int.class, classModel.getOriginClasses());
		}
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("setInteger2");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.unnamedElement.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.unnamedElement.get(0));
			Assertions.assertEquals(Integer.class, classModel.getOriginClasses());
		}
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("setString");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.unnamedElement.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.unnamedElement.get(0));
			Assertions.assertEquals(String.class, classModel.getOriginClasses());
		}
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("setObject");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.unnamedElement.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.unnamedElement.get(0));
			Assertions.assertEquals(TestObject.class, classModel.getOriginClasses());
		}
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("setEnum");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.unnamedElement.size());
			final ClassEnumModel classModel = Assertions.assertInstanceOf(ClassEnumModel.class,
					model.unnamedElement.get(0));
			Assertions.assertEquals(TestEnum.class, classModel.getOriginClasses());
		}

	}

	public class ListParameter {
		@GET
		public void setList(final List<Integer> parameter) {}
	}

	@Test
	public void testListParameter() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(ListParameter.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(1, api.apiModels.get(0).interfaces.size());
		final ApiModel model = api.apiModels.get(0).getInterfaceNamed("setList");
		Assertions.assertNotNull(model);
		Assertions.assertEquals(1, model.unnamedElement.size());
		final ClassListModel classModel = Assertions.assertInstanceOf(ClassListModel.class,
				model.unnamedElement.get(0));
		final ClassObjectModel classModelValue = Assertions.assertInstanceOf(ClassObjectModel.class,
				classModel.valueModel);
		Assertions.assertEquals(Integer.class, classModelValue.getOriginClasses());

	}

	public class MapParameter {
		@GET
		public void setMap(final Map<String, Integer> parameter) {}
	}

	@Test
	public void testMapParameter() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(MapParameter.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(1, api.apiModels.get(0).interfaces.size());
		final ApiModel model = api.apiModels.get(0).getInterfaceNamed("setMap");
		Assertions.assertNotNull(model);
		Assertions.assertEquals(1, model.unnamedElement.size());
		final ClassMapModel classModel = Assertions.assertInstanceOf(ClassMapModel.class, model.unnamedElement.get(0));
		final ClassObjectModel classModelKey = Assertions.assertInstanceOf(ClassObjectModel.class, classModel.keyModel);
		Assertions.assertEquals(String.class, classModelKey.getOriginClasses());
		final ClassObjectModel classModelValue = Assertions.assertInstanceOf(ClassObjectModel.class,
				classModel.valueModel);
		Assertions.assertEquals(Integer.class, classModelValue.getOriginClasses());

	}
}
