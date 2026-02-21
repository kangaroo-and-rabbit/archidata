package test.atriasoft.archidata.bean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.atriasoft.archidata.bean.accessor.LambdaAccessorFactory;
import org.atriasoft.archidata.bean.accessor.PropertyGetter;
import org.atriasoft.archidata.bean.accessor.PropertySetter;
import org.atriasoft.archidata.bean.accessor.TypedPropertyGetter;
import org.atriasoft.archidata.bean.accessor.TypedPropertySetter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LambdaAccessorFactory} — verifies that lambda-based property
 * accessors work correctly for various access patterns.
 */
class TestLambdaAccessorFactory {

	// ===== Test model classes =====

	public static class SimpleBean {
		public String name;
		public int age;
		public final String immutable = "fixed";

		private String title;

		public String getTitle() {
			return this.title;
		}

		public void setTitle(final String title) {
			this.title = title;
		}

		private boolean active;

		public boolean isActive() {
			return this.active;
		}

		public void setActive(final boolean active) {
			this.active = active;
		}
	}

	// ===== Method-based getter/setter =====

	@Test
	void testCreateGetterFromMethod() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("getTitle");
		final PropertyGetter pg = LambdaAccessorFactory.createGetter(getter);
		Assertions.assertNotNull(pg);

		final SimpleBean bean = new SimpleBean();
		bean.setTitle("Hello");
		Assertions.assertEquals("Hello", pg.get(bean));
	}

	@Test
	void testCreateSetterFromMethod() throws Throwable {
		final Method setter = SimpleBean.class.getMethod("setTitle", String.class);
		final PropertySetter ps = LambdaAccessorFactory.createSetter(setter);
		Assertions.assertNotNull(ps);

		final SimpleBean bean = new SimpleBean();
		ps.set(bean, "World");
		Assertions.assertEquals("World", bean.getTitle());
	}

	@Test
	void testCreateGetterBooleanIs() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("isActive");
		final PropertyGetter pg = LambdaAccessorFactory.createGetter(getter);

		final SimpleBean bean = new SimpleBean();
		bean.setActive(true);
		Assertions.assertEquals(true, pg.get(bean));
	}

	@Test
	void testCreateSetterBoolean() throws Throwable {
		final Method setter = SimpleBean.class.getMethod("setActive", boolean.class);
		final PropertySetter ps = LambdaAccessorFactory.createSetter(setter);

		final SimpleBean bean = new SimpleBean();
		ps.set(bean, true);
		Assertions.assertTrue(bean.isActive());
	}

	// ===== Field-based getter/setter =====

	@Test
	void testCreateFieldGetter() throws Throwable {
		final Field field = SimpleBean.class.getField("name");
		final PropertyGetter pg = LambdaAccessorFactory.createFieldGetter(field);
		Assertions.assertNotNull(pg);

		final SimpleBean bean = new SimpleBean();
		bean.name = "Alice";
		Assertions.assertEquals("Alice", pg.get(bean));
	}

	@Test
	void testCreateFieldSetter() throws Throwable {
		final Field field = SimpleBean.class.getField("name");
		final PropertySetter ps = LambdaAccessorFactory.createFieldSetter(field);
		Assertions.assertNotNull(ps);

		final SimpleBean bean = new SimpleBean();
		ps.set(bean, "Bob");
		Assertions.assertEquals("Bob", bean.name);
	}

	@Test
	void testCreateFieldSetterFinalReturnsNull() throws Exception {
		final Field field = SimpleBean.class.getField("immutable");
		final PropertySetter ps = LambdaAccessorFactory.createFieldSetter(field);
		Assertions.assertNull(ps, "Setter for final field should be null");
	}

	@Test
	void testCreateFieldGetterFinal() throws Throwable {
		final Field field = SimpleBean.class.getField("immutable");
		final PropertyGetter pg = LambdaAccessorFactory.createFieldGetter(field);
		Assertions.assertNotNull(pg);

		final SimpleBean bean = new SimpleBean();
		Assertions.assertEquals("fixed", pg.get(bean));
	}

	@Test
	void testCreateFieldGetterPrimitive() throws Throwable {
		final Field field = SimpleBean.class.getField("age");
		final PropertyGetter pg = LambdaAccessorFactory.createFieldGetter(field);

		final SimpleBean bean = new SimpleBean();
		bean.age = 42;
		Assertions.assertEquals(42, pg.get(bean));
	}

	@Test
	void testCreateFieldSetterPrimitive() throws Throwable {
		final Field field = SimpleBean.class.getField("age");
		final PropertySetter ps = LambdaAccessorFactory.createFieldSetter(field);
		Assertions.assertNotNull(ps);

		final SimpleBean bean = new SimpleBean();
		ps.set(bean, 99);
		Assertions.assertEquals(99, bean.age);
	}

	// ===== Typed getters/setters =====

	@Test
	void testCreateTypedGetterFromMethod() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("getTitle");
		final TypedPropertyGetter<SimpleBean, String> tg = LambdaAccessorFactory.createTypedGetter(
				getter, SimpleBean.class, String.class);

		final SimpleBean bean = new SimpleBean();
		bean.setTitle("Typed");
		final String result = tg.get(bean);
		Assertions.assertEquals("Typed", result);
	}

	@Test
	void testCreateTypedSetterFromMethod() throws Throwable {
		final Method setter = SimpleBean.class.getMethod("setTitle", String.class);
		final TypedPropertySetter<SimpleBean, String> ts = LambdaAccessorFactory.createTypedSetter(
				setter, SimpleBean.class, String.class);

		final SimpleBean bean = new SimpleBean();
		ts.set(bean, "TypedSet");
		Assertions.assertEquals("TypedSet", bean.getTitle());
	}

	@Test
	void testCreateTypedFieldGetter() throws Throwable {
		final Field field = SimpleBean.class.getField("name");
		final TypedPropertyGetter<SimpleBean, String> tg = LambdaAccessorFactory.createTypedFieldGetter(
				field, SimpleBean.class, String.class);

		final SimpleBean bean = new SimpleBean();
		bean.name = "TypedField";
		Assertions.assertEquals("TypedField", tg.get(bean));
	}

	@Test
	void testCreateTypedFieldSetter() throws Throwable {
		final Field field = SimpleBean.class.getField("name");
		final TypedPropertySetter<SimpleBean, String> ts = LambdaAccessorFactory.createTypedFieldSetter(
				field, SimpleBean.class, String.class);
		Assertions.assertNotNull(ts);

		final SimpleBean bean = new SimpleBean();
		ts.set(bean, "TypedFieldSet");
		Assertions.assertEquals("TypedFieldSet", bean.name);
	}

	@Test
	void testCreateTypedFieldSetterFinalReturnsNull() throws Exception {
		final Field field = SimpleBean.class.getField("immutable");
		final TypedPropertySetter<SimpleBean, String> ts = LambdaAccessorFactory.createTypedFieldSetter(
				field, SimpleBean.class, String.class);
		Assertions.assertNull(ts, "Typed setter for final field should be null");
	}

	// ===== Multiple instances consistency =====

	@Test
	void testMultipleInstancesAreIndependent() throws Throwable {
		final Field field = SimpleBean.class.getField("name");
		final PropertyGetter pg = LambdaAccessorFactory.createFieldGetter(field);
		final PropertySetter ps = LambdaAccessorFactory.createFieldSetter(field);

		final SimpleBean bean1 = new SimpleBean();
		final SimpleBean bean2 = new SimpleBean();

		ps.set(bean1, "one");
		ps.set(bean2, "two");

		Assertions.assertEquals("one", pg.get(bean1));
		Assertions.assertEquals("two", pg.get(bean2));
	}

	@Test
	void testNullValueHandling() throws Throwable {
		final Field field = SimpleBean.class.getField("name");
		final PropertyGetter pg = LambdaAccessorFactory.createFieldGetter(field);
		final PropertySetter ps = LambdaAccessorFactory.createFieldSetter(field);

		final SimpleBean bean = new SimpleBean();
		ps.set(bean, "notNull");
		Assertions.assertEquals("notNull", pg.get(bean));

		ps.set(bean, null);
		Assertions.assertNull(pg.get(bean));
	}
}
