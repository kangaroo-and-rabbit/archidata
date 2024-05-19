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
import jakarta.ws.rs.core.Response;

public class TestAnalyzeApiReturn {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestAnalyzeApiReturn.class);

	public enum TestEnum {
		PLOP, PLIP
	}

	public class TestObject {
		public int value;
	}

	public class ReturnValueVoid {
		@GET
		public void getVoid1() {}

		@GET
		public Void getVoid2() {
			return null;
		}
	}

	@Test
	public void testReturnVoid() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(ReturnValueVoid.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(2, api.apiModels.get(0).interfaces.size());
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getVoid1");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(void.class, classModel.getOriginClasses());
		}
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getVoid2");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(Void.class, classModel.getOriginClasses());
		}

	}

	public class ReturnValueInteger {
		@GET
		public int getInteger1() {
			return 0;
		}

		@GET
		public Integer getInteger2() {
			return 0;
		}

	}

	@Test
	public void testReturnInteger() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(ReturnValueInteger.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(2, api.apiModels.get(0).interfaces.size());
		// Check int
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getInteger1");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(int.class, classModel.getOriginClasses());
		}
		// Check Integer
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getInteger2");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(Integer.class, classModel.getOriginClasses());
		}

	}

	public class ReturnValueShort {
		@GET
		public short getShort1() {
			return 0;
		}

		@GET
		public Short getShort2() {
			return 0;
		}

	}

	@Test
	public void testReturnShort() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(ReturnValueShort.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(2, api.apiModels.get(0).interfaces.size());
		// Check short
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getShort1");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(short.class, classModel.getOriginClasses());
		}
		// Check Short
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getShort2");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(Short.class, classModel.getOriginClasses());
		}

	}

	public class ReturnValueLong {
		@GET
		public long getLong1() {
			return 0;
		}

		@GET
		public Long getLong2() {
			return 0L;
		}

	}

	@Test
	public void testReturnLong() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(ReturnValueLong.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(2, api.apiModels.get(0).interfaces.size());
		// Check long
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getLong1");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(long.class, classModel.getOriginClasses());
		}
		// Check Long
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getLong2");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			Assertions.assertInstanceOf(ClassObjectModel.class, model.returnTypes.get(0));
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(Long.class, classModel.getOriginClasses());
		}

	}

	public class ReturnValueFloat {
		@GET
		public float getFloat1() {
			return 0;
		}

		@GET
		public Float getFloat2() {
			return 0.0f;
		}

	}

	@Test
	public void testReturnFloat() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(ReturnValueFloat.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(2, api.apiModels.get(0).interfaces.size());
		// Check float
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getFloat1");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(float.class, classModel.getOriginClasses());
		}
		// Check Float
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getFloat2");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(Float.class, classModel.getOriginClasses());
		}

	}

	public class ReturnValueDouble {
		@GET
		public double getDouble1() {
			return 0;
		}

		@GET
		public Double getDouble2() {
			return 0.0;
		}

	}

	@Test
	public void testReturnDouble() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(ReturnValueDouble.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(2, api.apiModels.get(0).interfaces.size());
		// Check double
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getDouble1");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(double.class, classModel.getOriginClasses());
		}
		// Check Double
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getDouble2");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(Double.class, classModel.getOriginClasses());
		}

	}

	public class ReturnValueString {
		@GET
		public String getString() {
			return "0";
		}

	}

	@Test
	public void testReturnString() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(ReturnValueString.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(1, api.apiModels.get(0).interfaces.size());
		// Check String
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getString");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(String.class, classModel.getOriginClasses());
		}

	}

	public class ReturnValueAny {
		@GET
		public Response getResponse() {
			return null;
		}

		@GET
		public Object getObject() {
			return null;
		}
	}

	@Test
	public void testReturnAny() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(ReturnValueAny.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(2, api.apiModels.get(0).interfaces.size());
		// Check Response ==> represent a Any value then it wrapped as Object
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getResponse");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(Object.class, classModel.getOriginClasses());
		}
		// Check Object
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getObject");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassObjectModel classModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(Object.class, classModel.getOriginClasses());
		}

	}

	public class ReturnValueEnum {
		@GET
		public TestEnum getEnum() {
			return TestEnum.PLIP;
		}

	}

	@Test
	public void testReturnEnum() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(ReturnValueEnum.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(1, api.apiModels.get(0).interfaces.size());
		// Check Enum
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getEnum");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			final ClassEnumModel classModel = Assertions.assertInstanceOf(ClassEnumModel.class,
					model.returnTypes.get(0));
			Assertions.assertEquals(TestEnum.class, classModel.getOriginClasses());
		}
	}

	public class ReturnValueList {
		@GET
		public List<Integer> getListInteger() {
			return null;
		}

		@GET
		public List<TestEnum> getListEnum() {
			return null;
		}

		@GET
		public List<TestObject> getListObject() {
			return null;
		}

		@GET
		public List<List<Integer>> getListListInteger() {
			return null;
		}

		@GET
		public List<Map<String, Integer>> getListMapInteger() {
			return null;
		}

	}

	@Test
	public void testReturnList() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(ReturnValueList.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(5, api.apiModels.get(0).interfaces.size());
		// Check List<Integer>
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getListInteger");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			// Level 0
			final ClassListModel classListModel = Assertions.assertInstanceOf(ClassListModel.class,
					model.returnTypes.get(0));
			// Level 1
			final ClassObjectModel classModelOfValue = Assertions.assertInstanceOf(ClassObjectModel.class,
					classListModel.valueModel);
			Assertions.assertEquals(Integer.class, classModelOfValue.getOriginClasses());
		}
		// Check List<TestEnum>
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getListEnum");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			// Level 0
			final ClassListModel classListModel = Assertions.assertInstanceOf(ClassListModel.class,
					model.returnTypes.get(0));
			// Level 1
			final ClassEnumModel classModelOfValue = Assertions.assertInstanceOf(ClassEnumModel.class,
					classListModel.valueModel);
			Assertions.assertEquals(TestEnum.class, classModelOfValue.getOriginClasses());
		}
		// Check List<TestObject>
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getListObject");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			// Level 0
			final ClassListModel classListModel = Assertions.assertInstanceOf(ClassListModel.class,
					model.returnTypes.get(0));
			// Level 1
			final ClassObjectModel classModelOfValue = Assertions.assertInstanceOf(ClassObjectModel.class,
					classListModel.valueModel);
			Assertions.assertEquals(Integer.class, classModelOfValue.getOriginClasses());
		}
		// Check List<List<Integer>>
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getListListInteger");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			// Level 0
			final ClassListModel classListModel = Assertions.assertInstanceOf(ClassListModel.class,
					model.returnTypes.get(0));
			// Level 1
			final ClassListModel classList2Model = Assertions.assertInstanceOf(ClassListModel.class,
					classListModel.valueModel);
			// Level 2
			final ClassObjectModel classModelOfValue = Assertions.assertInstanceOf(ClassObjectModel.class,
					classList2Model.valueModel);
			Assertions.assertEquals(Integer.class, classModelOfValue.getOriginClasses());
		}
		// Check List<Map<String, Integer>>
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getListMapInteger");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			// Level 0
			final ClassListModel classListModel = Assertions.assertInstanceOf(ClassListModel.class,
					model.returnTypes.get(0));
			// Level 1
			final ClassMapModel classMapModel = Assertions.assertInstanceOf(ClassMapModel.class,
					classListModel.valueModel);
			// Level 2
			final ClassObjectModel classModelOfKey = Assertions.assertInstanceOf(ClassObjectModel.class,
					classMapModel.keyModel);
			Assertions.assertEquals(String.class, classModelOfKey.getOriginClasses());
			final ClassObjectModel classModelOfValue = Assertions.assertInstanceOf(ClassObjectModel.class,
					classMapModel.valueModel);
			Assertions.assertEquals(Integer.class, classModelOfValue.getOriginClasses());
		}

	}

	// does not test other than key string, but in theory it works.
	public class ReturnValueMap {
		@GET
		public Map<String, Integer> getMapInteger() {
			return null;
		}

		@GET
		public Map<String, TestEnum> getMapEnum() {
			return null;
		}

		@GET
		public Map<String, TestObject> getMapObject() {
			return null;
		}

		@GET
		public Map<String, Map<String, Integer>> getMapMap() {
			return null;
		}

		@GET
		public Map<String, List<Integer>> getMapList() {
			return null;
		}

	}

	@Test
	public void testReturnMap() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.createApi(List.of(ReturnValueMap.class));

		Assertions.assertEquals(1, api.apiModels.size());
		Assertions.assertEquals(5, api.apiModels.get(0).interfaces.size());
		// Check Map<String, Integer>
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getMapInteger");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			// Level 0
			final ClassMapModel classMapModel = Assertions.assertInstanceOf(ClassMapModel.class,
					model.returnTypes.get(0));
			final ClassObjectModel classModelOfKey = Assertions.assertInstanceOf(ClassObjectModel.class,
					classMapModel.keyModel);
			Assertions.assertEquals(String.class, classModelOfKey.getOriginClasses());
			// Level 1
			final ClassObjectModel classModelOfValue = Assertions.assertInstanceOf(ClassObjectModel.class,
					classMapModel.valueModel);
			Assertions.assertEquals(Integer.class, classModelOfValue.getOriginClasses());
		}
		// Check Map<String, TestEnum>
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getMapEnum");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			// Level 0
			final ClassMapModel classMapModel = Assertions.assertInstanceOf(ClassMapModel.class,
					model.returnTypes.get(0));
			final ClassObjectModel classModelOfKey = Assertions.assertInstanceOf(ClassObjectModel.class,
					classMapModel.keyModel);
			Assertions.assertEquals(String.class, classModelOfKey.getOriginClasses());
			// Level 1
			final ClassEnumModel classModelOfValue = Assertions.assertInstanceOf(ClassEnumModel.class,
					classMapModel.valueModel);
			Assertions.assertEquals(TestEnum.class, classModelOfValue.getOriginClasses());
		}
		// Check Map<String, TestObject>
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getMapObject");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			// Level 0
			final ClassMapModel classMapModel = Assertions.assertInstanceOf(ClassMapModel.class,
					model.returnTypes.get(0));
			final ClassObjectModel classModelOfKey = Assertions.assertInstanceOf(ClassObjectModel.class,
					classMapModel.keyModel);
			Assertions.assertEquals(String.class, classModelOfKey.getOriginClasses());
			// Level 1
			final ClassObjectModel classModelOfValue = Assertions.assertInstanceOf(ClassObjectModel.class,
					classMapModel.valueModel);
			Assertions.assertEquals(TestObject.class, classModelOfValue.getOriginClasses());
		}
		// Check Map<String, Map<String, Integer>>
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getMapMap");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			// Level 0
			final ClassMapModel classMapModel = Assertions.assertInstanceOf(ClassMapModel.class,
					model.returnTypes.get(0));
			final ClassObjectModel classModelOfKey = Assertions.assertInstanceOf(ClassObjectModel.class,
					classMapModel.keyModel);
			Assertions.assertEquals(String.class, classModelOfKey.getOriginClasses());
			// Level 1
			final ClassMapModel classModelOfValue = Assertions.assertInstanceOf(ClassMapModel.class,
					classMapModel.valueModel);
			final ClassObjectModel classModelOfValueOfKey = Assertions.assertInstanceOf(ClassObjectModel.class,
					classModelOfValue.keyModel);
			Assertions.assertEquals(String.class, classModelOfValueOfKey.getOriginClasses());

			final ClassObjectModel classSubModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					classModelOfValue.valueModel);
			Assertions.assertEquals(Integer.class, classSubModel.getOriginClasses());
		}
		// Check Map<String, List<Integer>>
		{
			final ApiModel model = api.apiModels.get(0).getInterfaceNamed("getMapList");
			Assertions.assertNotNull(model);
			Assertions.assertEquals(1, model.returnTypes.size());
			// Level 0
			final ClassMapModel classMapModel = Assertions.assertInstanceOf(ClassMapModel.class,
					model.returnTypes.get(0));
			final ClassObjectModel classModelOfKey = Assertions.assertInstanceOf(ClassObjectModel.class,
					classMapModel.keyModel);
			Assertions.assertEquals(String.class, classModelOfKey.getOriginClasses());
			// Level 1
			final ClassListModel classModelOfValue = Assertions.assertInstanceOf(ClassListModel.class,
					classMapModel.valueModel);
			final ClassObjectModel classSubModel = Assertions.assertInstanceOf(ClassObjectModel.class,
					classModelOfValue.valueModel);
			Assertions.assertEquals(Integer.class, classSubModel.getOriginClasses());
		}

	}

}
