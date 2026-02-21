package test.atriasoft.archidata.bean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.atriasoft.archidata.bean.TypeInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TypeInfo} — verifies type resolution for simple types,
 * generics, collections, maps, arrays, enums, records, and edge cases.
 */
class TestTypeInfo {

	// ===== Test model =====

	public static class TypeModel {
		public String simple;
		public int primitive;
		public List<String> listOfString;
		public Set<Integer> setOfInteger;
		public Map<String, Long> mapStringLong;
		public Map<Integer, List<String>> mapIntListString;
		public List<List<String>> nestedList;
		public Optional<String> optionalString;
		public int[] intArray;
		public String[] stringArray;
		public TestEnum enumField;
	}

	public record TestRecord(String name, int age) {}

	public enum TestEnum {
		A, B
	}

	// ===== ofRaw =====

	@Test
	void testOfRawSimpleClass() {
		final TypeInfo ti = TypeInfo.ofRaw(String.class);
		Assertions.assertEquals(String.class, ti.rawType());
		Assertions.assertNull(ti.elementType());
		Assertions.assertNull(ti.keyType());
		Assertions.assertFalse(ti.isList());
		Assertions.assertFalse(ti.isMap());
		Assertions.assertFalse(ti.isArray());
	}

	@Test
	void testOfRawPrimitive() {
		final TypeInfo ti = TypeInfo.ofRaw(int.class);
		Assertions.assertEquals(int.class, ti.rawType());
		Assertions.assertTrue(ti.isPrimitive());
		Assertions.assertFalse(ti.isEnum());
	}

	@Test
	void testOfRawArray() {
		final TypeInfo ti = TypeInfo.ofRaw(int[].class);
		Assertions.assertTrue(ti.isArray());
		Assertions.assertEquals(int.class, ti.elementType());
		Assertions.assertEquals(int[].class, ti.rawType());
	}

	@Test
	void testOfRawEnum() {
		final TypeInfo ti = TypeInfo.ofRaw(TestEnum.class);
		Assertions.assertTrue(ti.isEnum());
		Assertions.assertFalse(ti.isPrimitive());
	}

	@Test
	void testOfRawRecord() {
		final TypeInfo ti = TypeInfo.ofRaw(TestRecord.class);
		Assertions.assertTrue(ti.isRecord());
		Assertions.assertFalse(ti.isEnum());
	}

	// ===== fromField =====

	@Test
	void testFromFieldSimple() throws Exception {
		final Field field = TypeModel.class.getField("simple");
		final TypeInfo ti = TypeInfo.fromField(field);
		Assertions.assertEquals(String.class, ti.rawType());
		Assertions.assertNull(ti.elementType());
	}

	@Test
	void testFromFieldPrimitive() throws Exception {
		final Field field = TypeModel.class.getField("primitive");
		final TypeInfo ti = TypeInfo.fromField(field);
		Assertions.assertEquals(int.class, ti.rawType());
		Assertions.assertTrue(ti.isPrimitive());
	}

	@Test
	void testFromFieldList() throws Exception {
		final Field field = TypeModel.class.getField("listOfString");
		final TypeInfo ti = TypeInfo.fromField(field);
		Assertions.assertTrue(ti.isList());
		Assertions.assertTrue(ti.isCollection());
		Assertions.assertEquals(List.class, ti.rawType());
		Assertions.assertEquals(String.class, ti.elementType());
	}

	@Test
	void testFromFieldSet() throws Exception {
		final Field field = TypeModel.class.getField("setOfInteger");
		final TypeInfo ti = TypeInfo.fromField(field);
		Assertions.assertTrue(ti.isSet());
		Assertions.assertTrue(ti.isCollection());
		Assertions.assertEquals(Set.class, ti.rawType());
		Assertions.assertEquals(Integer.class, ti.elementType());
	}

	@Test
	void testFromFieldMap() throws Exception {
		final Field field = TypeModel.class.getField("mapStringLong");
		final TypeInfo ti = TypeInfo.fromField(field);
		Assertions.assertTrue(ti.isMap());
		Assertions.assertFalse(ti.isList());
		Assertions.assertEquals(Map.class, ti.rawType());
		Assertions.assertEquals(String.class, ti.keyType());
		Assertions.assertEquals(Long.class, ti.elementType());
	}

	@Test
	void testFromFieldNestedList() throws Exception {
		final Field field = TypeModel.class.getField("nestedList");
		final TypeInfo ti = TypeInfo.fromField(field);
		Assertions.assertTrue(ti.isList());
		Assertions.assertEquals(List.class, ti.rawType());
		// elementType for nested List<List<String>> → the inner List.class
		Assertions.assertEquals(List.class, ti.elementType());
	}

	@Test
	void testFromFieldMapWithComplexValue() throws Exception {
		final Field field = TypeModel.class.getField("mapIntListString");
		final TypeInfo ti = TypeInfo.fromField(field);
		Assertions.assertTrue(ti.isMap());
		Assertions.assertEquals(Integer.class, ti.keyType());
		// elementType is List.class (the value type raw class)
		Assertions.assertEquals(List.class, ti.elementType());
	}

	@Test
	void testFromFieldOptional() throws Exception {
		final Field field = TypeModel.class.getField("optionalString");
		final TypeInfo ti = TypeInfo.fromField(field);
		Assertions.assertTrue(ti.isOptional());
		Assertions.assertEquals(Optional.class, ti.rawType());
		Assertions.assertEquals(String.class, ti.elementType());
	}

	@Test
	void testFromFieldArray() throws Exception {
		final Field field = TypeModel.class.getField("intArray");
		final TypeInfo ti = TypeInfo.fromField(field);
		Assertions.assertTrue(ti.isArray());
		Assertions.assertEquals(int[].class, ti.rawType());
		Assertions.assertEquals(int.class, ti.elementType());
	}

	@Test
	void testFromFieldStringArray() throws Exception {
		final Field field = TypeModel.class.getField("stringArray");
		final TypeInfo ti = TypeInfo.fromField(field);
		Assertions.assertTrue(ti.isArray());
		Assertions.assertEquals(String[].class, ti.rawType());
		Assertions.assertEquals(String.class, ti.elementType());
	}

	@Test
	void testFromFieldEnum() throws Exception {
		final Field field = TypeModel.class.getField("enumField");
		final TypeInfo ti = TypeInfo.fromField(field);
		Assertions.assertTrue(ti.isEnum());
		Assertions.assertEquals(TestEnum.class, ti.rawType());
	}

	// ===== fromReturnType =====

	public static class MethodModel {
		public List<String> getNames() {
			return null;
		}

		public void setAge(final int age) {}

		public Map<String, Integer> getScores() {
			return null;
		}
	}

	@Test
	void testFromReturnType() throws Exception {
		final Method method = MethodModel.class.getMethod("getNames");
		final TypeInfo ti = TypeInfo.fromReturnType(method);
		Assertions.assertTrue(ti.isList());
		Assertions.assertEquals(String.class, ti.elementType());
	}

	@Test
	void testFromReturnTypeMap() throws Exception {
		final Method method = MethodModel.class.getMethod("getScores");
		final TypeInfo ti = TypeInfo.fromReturnType(method);
		Assertions.assertTrue(ti.isMap());
		Assertions.assertEquals(String.class, ti.keyType());
		Assertions.assertEquals(Integer.class, ti.elementType());
	}

	// ===== fromFirstParameter =====

	@Test
	void testFromFirstParameter() throws Exception {
		final Method method = MethodModel.class.getMethod("setAge", int.class);
		final TypeInfo ti = TypeInfo.fromFirstParameter(method);
		Assertions.assertEquals(int.class, ti.rawType());
		Assertions.assertTrue(ti.isPrimitive());
	}

	@Test
	void testFromFirstParameterNoParams() throws Exception {
		final Method method = MethodModel.class.getMethod("getNames");
		Assertions.assertThrows(IllegalArgumentException.class, () -> TypeInfo.fromFirstParameter(method));
	}

	// ===== fromType =====

	@Test
	void testFromTypeWithClass() {
		final TypeInfo ti = TypeInfo.fromType(String.class);
		Assertions.assertEquals(String.class, ti.rawType());
	}

	@Test
	void testFromTypeWithParameterizedType() throws Exception {
		// Get a ParameterizedType from a field's generic type
		final Field field = TypeModel.class.getField("listOfString");
		final Type genericType = field.getGenericType();
		final TypeInfo ti = TypeInfo.fromType(genericType);
		Assertions.assertTrue(ti.isList());
		Assertions.assertEquals(List.class, ti.rawType());
		Assertions.assertEquals(String.class, ti.elementType());
	}

	@Test
	void testFromTypeWithMapParameterizedType() throws Exception {
		final Field field = TypeModel.class.getField("mapStringLong");
		final Type genericType = field.getGenericType();
		final TypeInfo ti = TypeInfo.fromType(genericType);
		Assertions.assertTrue(ti.isMap());
		Assertions.assertEquals(String.class, ti.keyType());
		Assertions.assertEquals(Long.class, ti.elementType());
	}

	// ===== fromConstructorParameter =====

	@Test
	void testFromConstructorParameter() throws Exception {
		final var ctor = TestRecord.class.getDeclaredConstructors()[0];
		final TypeInfo ti0 = TypeInfo.fromConstructorParameter(ctor, 0);
		Assertions.assertEquals(String.class, ti0.rawType());
		final TypeInfo ti1 = TypeInfo.fromConstructorParameter(ctor, 1);
		Assertions.assertEquals(int.class, ti1.rawType());
		Assertions.assertTrue(ti1.isPrimitive());
	}

	// ===== Boolean checks =====

	@Test
	void testBooleanChecks() {
		Assertions.assertTrue(TypeInfo.ofRaw(List.class).isList());
		Assertions.assertFalse(TypeInfo.ofRaw(List.class).isSet());
		Assertions.assertTrue(TypeInfo.ofRaw(List.class).isCollection());

		Assertions.assertTrue(TypeInfo.ofRaw(Set.class).isSet());
		Assertions.assertFalse(TypeInfo.ofRaw(Set.class).isList());
		Assertions.assertTrue(TypeInfo.ofRaw(Set.class).isCollection());

		Assertions.assertTrue(TypeInfo.ofRaw(Map.class).isMap());
		Assertions.assertFalse(TypeInfo.ofRaw(Map.class).isList());
		Assertions.assertFalse(TypeInfo.ofRaw(Map.class).isCollection());
	}
}
