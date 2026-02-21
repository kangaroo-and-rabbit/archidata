package test.atriasoft.archidata.bean;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.atriasoft.archidata.bean.ClassModel;
import org.atriasoft.archidata.bean.ConstructorDescriptor;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.bean.TypeInfo;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestClassModel {

	@BeforeEach
	void clearCache() {
		ClassModel.clearCache();
	}

	// ===== Test model classes =====

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD, ElementType.METHOD })
	@interface MyAnnotation {
		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@interface MyClassAnnotation {
		String value() default "";
	}

	// Simple POJO with public fields
	@MyClassAnnotation("test")
	public static class SimplePojo {
		public String name;
		public int age;
		public List<String> tags;
	}

	// Class with getters/setters
	public static class GetterSetterClass {
		private String firstName;
		private boolean active;

		public String getFirstName() {
			return this.firstName;
		}

		public void setFirstName(final String firstName) {
			this.firstName = firstName;
		}

		public boolean isActive() {
			return this.active;
		}

		public void setActive(final boolean active) {
			this.active = active;
		}
	}

	// Record
	public record SimpleRecord(String name, int age, List<String> tags) {}

	// Enum
	public enum SimpleEnum {
		VALUE_A, VALUE_B, VALUE_C
	}

	// Class with annotations on field AND getter
	public static class AnnotatedClass {
		@MyAnnotation("on-field")
		public String name;

		private int count;

		@MyAnnotation("on-getter")
		public int getCount() {
			return this.count;
		}

		public void setCount(final int count) {
			this.count = count;
		}
	}

	// Interface with annotation
	public interface AnnotatedInterface {
		@MyAnnotation("on-interface")
		String getTitle();
	}

	// Class implementing annotated interface
	public static class ImplementingClass implements AnnotatedInterface {
		private String title;

		@Override
		public String getTitle() {
			return this.title;
		}

		public void setTitle(final String title) {
			this.title = title;
		}
	}

	// Class with Map and Set
	public static class CollectionClass {
		public Map<String, Integer> scores;
		public Set<UUID> ids;
		public List<List<String>> nested;
	}

	// Class with final field
	public static class FinalFieldClass {
		public final String immutable = "fixed";
		public String mutable;
	}

	// ===== Tests =====

	@Test
	void testSimplePojoIntrospection() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimplePojo.class);

		Assertions.assertEquals("SimplePojo", model.getSimpleName());
		Assertions.assertFalse(model.isRecord());
		Assertions.assertFalse(model.isEnum());
		Assertions.assertEquals(3, model.getProperties().size());

		// Check properties by name (sorted)
		Assertions.assertNotNull(model.getProperty("age"));
		Assertions.assertNotNull(model.getProperty("name"));
		Assertions.assertNotNull(model.getProperty("tags"));

		// Check types
		Assertions.assertEquals(int.class, model.getProperty("age").getType());
		Assertions.assertEquals(String.class, model.getProperty("name").getType());
		Assertions.assertEquals(List.class, model.getProperty("tags").getType());
		Assertions.assertEquals(String.class, model.getProperty("tags").getElementType());
	}

	@Test
	void testPojoReadWrite() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimplePojo.class);
		final SimplePojo obj = new SimplePojo();

		model.getProperty("name").setValue(obj, "Alice");
		model.getProperty("age").setValue(obj, 30);

		Assertions.assertEquals("Alice", model.getProperty("name").getValue(obj));
		Assertions.assertEquals(30, model.getProperty("age").getValue(obj));
	}

	@Test
	void testGetterSetterIntrospection() throws IntrospectionException {
		final ClassModel model = ClassModel.of(GetterSetterClass.class);

		Assertions.assertEquals(2, model.getProperties().size());
		final PropertyDescriptor active = model.getProperty("active");
		final PropertyDescriptor firstName = model.getProperty("firstName");

		Assertions.assertNotNull(active);
		Assertions.assertNotNull(firstName);
		Assertions.assertEquals(boolean.class, active.getType());
		Assertions.assertEquals(String.class, firstName.getType());

		// Test read/write via lambda
		final GetterSetterClass obj = new GetterSetterClass();
		firstName.setValue(obj, "Bob");
		active.setValue(obj, true);

		Assertions.assertEquals("Bob", firstName.getValue(obj));
		Assertions.assertEquals(true, active.getValue(obj));
	}

	@Test
	void testRecordIntrospection() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimpleRecord.class);

		Assertions.assertTrue(model.isRecord());
		Assertions.assertEquals(3, model.getProperties().size());

		final PropertyDescriptor name = model.getProperty("name");
		Assertions.assertNotNull(name);
		Assertions.assertTrue(name.canRead());
		Assertions.assertTrue(name.isReadOnly()); // Records are immutable

		// Test reading from record
		final SimpleRecord rec = new SimpleRecord("Charlie", 25, List.of("a", "b"));
		Assertions.assertEquals("Charlie", name.getValue(rec));
		Assertions.assertEquals(25, model.getProperty("age").getValue(rec));
	}

	@Test
	void testEnumIntrospection() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimpleEnum.class);
		Assertions.assertTrue(model.isEnum());
	}

	@Test
	void testClassAnnotation() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimplePojo.class);
		Assertions.assertTrue(model.hasClassAnnotation(MyClassAnnotation.class));
		final MyClassAnnotation ann = model.getClassAnnotation(MyClassAnnotation.class);
		Assertions.assertNotNull(ann);
		Assertions.assertEquals("test", ann.value());
	}

	@Test
	void testFieldAnnotation() throws IntrospectionException {
		final ClassModel model = ClassModel.of(AnnotatedClass.class);

		final PropertyDescriptor name = model.getProperty("name");
		Assertions.assertTrue(name.hasAnnotation(MyAnnotation.class));
		Assertions.assertEquals("on-field", name.getAnnotation(MyAnnotation.class).value());

		final PropertyDescriptor count = model.getProperty("count");
		Assertions.assertTrue(count.hasAnnotation(MyAnnotation.class));
		Assertions.assertEquals("on-getter", count.getAnnotation(MyAnnotation.class).value());
	}

	@Test
	void testInterfaceAnnotationInheritance() throws IntrospectionException {
		final ClassModel model = ClassModel.of(ImplementingClass.class);

		final PropertyDescriptor title = model.getProperty("title");
		Assertions.assertNotNull(title);
		// The annotation is on the interface method, should be found via unified lookup
		Assertions.assertTrue(title.hasAnnotation(MyAnnotation.class));
		Assertions.assertEquals("on-interface", title.getAnnotation(MyAnnotation.class).value());
	}

	@Test
	void testMapType() throws IntrospectionException {
		final ClassModel model = ClassModel.of(CollectionClass.class);

		final PropertyDescriptor scores = model.getProperty("scores");
		Assertions.assertNotNull(scores);
		final TypeInfo ti = scores.getTypeInfo();
		Assertions.assertTrue(ti.isMap());
		Assertions.assertEquals(String.class, ti.keyType());
		Assertions.assertEquals(Integer.class, ti.elementType());
	}

	@Test
	void testSetType() throws IntrospectionException {
		final ClassModel model = ClassModel.of(CollectionClass.class);

		final PropertyDescriptor ids = model.getProperty("ids");
		Assertions.assertNotNull(ids);
		Assertions.assertTrue(ids.getTypeInfo().isSet());
		Assertions.assertEquals(UUID.class, ids.getElementType());
	}

	@Test
	void testFinalField() throws IntrospectionException {
		final ClassModel model = ClassModel.of(FinalFieldClass.class);

		final PropertyDescriptor immutable = model.getProperty("immutable");
		Assertions.assertTrue(immutable.canRead());
		Assertions.assertTrue(immutable.isReadOnly());
		// Should be able to read the final field
		final FinalFieldClass obj = new FinalFieldClass();
		Assertions.assertEquals("fixed", immutable.getValue(obj));
	}

	@Test
	void testCaching() throws IntrospectionException {
		final ClassModel model1 = ClassModel.of(SimplePojo.class);
		final ClassModel model2 = ClassModel.of(SimplePojo.class);
		Assertions.assertSame(model1, model2, "ClassModel should be cached and return same instance");
	}

	@Test
	void testDefaultConstructor() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimplePojo.class);
		Assertions.assertNotNull(model.getDefaultConstructor());
		Assertions.assertTrue(model.getDefaultConstructor().isNoArg());

		final Object instance = model.newInstance();
		Assertions.assertInstanceOf(SimplePojo.class, instance);
	}

	@Test
	void testNewInstanceWithValues() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimplePojo.class);
		final Object instance = model.newInstance(Map.of("name", "Dave", "age", 42));

		Assertions.assertInstanceOf(SimplePojo.class, instance);
		final SimplePojo pojo = (SimplePojo) instance;
		Assertions.assertEquals("Dave", pojo.name);
		Assertions.assertEquals(42, pojo.age);
	}

	@Test
	void testRecordConstructor() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimpleRecord.class);

		// Record should have a constructor with all parameters
		Assertions.assertFalse(model.getConstructors().isEmpty());
		final ConstructorDescriptor ctor = model.getConstructors().get(0);
		Assertions.assertEquals(3, ctor.parameterCount());
	}

	@Test
	void testFindPropertyWithAnnotation() throws IntrospectionException {
		final ClassModel model = ClassModel.of(AnnotatedClass.class);

		final PropertyDescriptor found = model.findPropertyWithAnnotation(MyAnnotation.class);
		Assertions.assertNotNull(found);
		// Should find one of the annotated properties (count or name)
		Assertions.assertTrue(found.hasAnnotation(MyAnnotation.class));

		final List<PropertyDescriptor> all = model.findPropertiesWithAnnotation(MyAnnotation.class);
		Assertions.assertEquals(2, all.size());
	}

	@Test
	void testTypeInfoFactoryMethods() {
		// Test TypeInfo.ofRaw
		final TypeInfo raw = TypeInfo.ofRaw(String.class);
		Assertions.assertEquals(String.class, raw.rawType());
		Assertions.assertNull(raw.elementType());
		Assertions.assertFalse(raw.isList());

		// Test array
		final TypeInfo arr = TypeInfo.ofRaw(int[].class);
		Assertions.assertTrue(arr.isArray());
		Assertions.assertEquals(int.class, arr.elementType());
	}

	// ===== Additional tests for enhanced coverage =====

	// Class with inheritance
	public static class ParentClass {
		public String parentField;
	}

	public static class ChildClass extends ParentClass {
		public String childField;
	}

	@Test
	void testInheritanceProperties() throws IntrospectionException {
		final ClassModel model = ClassModel.of(ChildClass.class);
		// ClassModel introspects declared fields, child should have childField
		Assertions.assertNotNull(model.getProperty("childField"));
		// parentField is inherited — depends on ClassModel behavior
	}

	// Class with multiple constructors
	public static class MultiConstructor {
		public String name;
		public int age;

		public MultiConstructor() {}

		public MultiConstructor(final String name) {
			this.name = name;
		}

		public MultiConstructor(final String name, final int age) {
			this.name = name;
			this.age = age;
		}
	}

	@Test
	void testMultipleConstructors() throws IntrospectionException {
		final ClassModel model = ClassModel.of(MultiConstructor.class);
		// Should detect multiple constructors (sorted by parameter count)
		Assertions.assertTrue(model.getConstructors().size() >= 2);
		Assertions.assertNotNull(model.getDefaultConstructor());
		Assertions.assertTrue(model.getDefaultConstructor().isNoArg());
	}

	@Test
	void testNewInstanceDefaultConstructor() throws IntrospectionException {
		final ClassModel model = ClassModel.of(MultiConstructor.class);
		final Object instance = model.newInstance();
		Assertions.assertInstanceOf(MultiConstructor.class, instance);
	}

	// Class with only getter (no setter, no field)
	public static class GetterOnlyClass {
		public String getComputed() {
			return "computed";
		}
	}

	@Test
	void testGetterOnlyProperty() throws IntrospectionException {
		final ClassModel model = ClassModel.of(GetterOnlyClass.class);
		final PropertyDescriptor prop = model.getProperty("computed");
		Assertions.assertNotNull(prop);
		Assertions.assertTrue(prop.canRead());
		Assertions.assertFalse(prop.canWrite());

		// Read value
		final GetterOnlyClass obj = new GetterOnlyClass();
		Assertions.assertEquals("computed", prop.getValue(obj));
	}

	// Test property not found
	@Test
	void testPropertyNotFound() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimplePojo.class);
		Assertions.assertNull(model.getProperty("nonExistent"));
	}

	// Test record instantiation with newInstance(Map)
	@Test
	void testRecordNewInstanceWithValues() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimpleRecord.class);
		final Object instance = model.newInstance(Map.of("name", "Alice", "age", 25, "tags", List.of("a")));
		Assertions.assertInstanceOf(SimpleRecord.class, instance);
		final SimpleRecord rec = (SimpleRecord) instance;
		Assertions.assertEquals("Alice", rec.name());
		Assertions.assertEquals(25, rec.age());
		Assertions.assertEquals(List.of("a"), rec.tags());
	}

	// Test nested list type
	@Test
	void testNestedListType() throws IntrospectionException {
		final ClassModel model = ClassModel.of(CollectionClass.class);
		final PropertyDescriptor nested = model.getProperty("nested");
		Assertions.assertNotNull(nested);
		Assertions.assertTrue(nested.getTypeInfo().isList());
		// Element type of List<List<String>> is List
		Assertions.assertEquals(List.class, nested.getElementType());
	}

	// Test PropertyDescriptor toString
	@Test
	void testPropertyDescriptorToString() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimplePojo.class);
		final PropertyDescriptor prop = model.getProperty("name");
		final String str = prop.toString();
		Assertions.assertTrue(str.contains("name"));
		Assertions.assertTrue(str.contains("String"));
	}

	// Test setValue on read-only throws
	@Test
	void testSetValueOnReadOnlyThrows() throws IntrospectionException {
		final ClassModel model = ClassModel.of(FinalFieldClass.class);
		final PropertyDescriptor immutable = model.getProperty("immutable");
		final FinalFieldClass obj = new FinalFieldClass();
		Assertions.assertThrows(Exception.class, () -> immutable.setValue(obj, "newValue"));
	}

	// Test getValue with no getter throws
	@Test
	void testFindPropertyWithAnnotationNotFound() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimplePojo.class);
		final PropertyDescriptor found = model.findPropertyWithAnnotation(MyAnnotation.class);
		Assertions.assertNull(found, "No property should have @MyAnnotation in SimplePojo");
	}

	// ConstructorDescriptor tests
	@Test
	void testConstructorDescriptorIndexOfParameter() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimpleRecord.class);
		final ConstructorDescriptor ctor = model.getConstructors().get(0);
		Assertions.assertTrue(ctor.indexOfParameter("name") >= 0);
		Assertions.assertTrue(ctor.indexOfParameter("age") >= 0);
		Assertions.assertEquals(-1, ctor.indexOfParameter("nonExistent"));
	}

	@Test
	void testConstructorDescriptorNewInstance() throws IntrospectionException {
		final ClassModel model = ClassModel.of(SimpleRecord.class);
		final ConstructorDescriptor ctor = model.getConstructors().get(0);
		// Find parameter order
		final int nameIdx = ctor.indexOfParameter("name");
		final int ageIdx = ctor.indexOfParameter("age");
		final int tagsIdx = ctor.indexOfParameter("tags");
		final Object[] args = new Object[3];
		args[nameIdx] = "Test";
		args[ageIdx] = 42;
		args[tagsIdx] = List.of("x");
		final Object instance = ctor.newInstance(args);
		Assertions.assertInstanceOf(SimpleRecord.class, instance);
		final SimpleRecord rec = (SimpleRecord) instance;
		Assertions.assertEquals("Test", rec.name());
		Assertions.assertEquals(42, rec.age());
	}
}
