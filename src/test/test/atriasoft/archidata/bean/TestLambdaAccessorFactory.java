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

		private long timestamp;

		public long getTimestamp() {
			return this.timestamp;
		}

		public void setTimestamp(final long timestamp) {
			this.timestamp = timestamp;
		}

		private int count;

		public int getCount() {
			return this.count;
		}

		public void setCount(final int count) {
			this.count = count;
		}

		private double ratio;

		public double getRatio() {
			return this.ratio;
		}

		public void setRatio(final double ratio) {
			this.ratio = ratio;
		}

		private float score;

		public float getScore() {
			return this.score;
		}

		public void setScore(final float score) {
			this.score = score;
		}

		private short level;

		public short getLevel() {
			return this.level;
		}

		public void setLevel(final short level) {
			this.level = level;
		}

		private byte flags;

		public byte getFlags() {
			return this.flags;
		}

		public void setFlags(final byte flags) {
			this.flags = flags;
		}

		private char initial;

		public char getInitial() {
			return this.initial;
		}

		public void setInitial(final char initial) {
			this.initial = initial;
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

	// ===== Primitive method-based getter/setter (all types) =====

	@Test
	void testGetterSetterPrimitiveLong() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("getTimestamp");
		final Method setter = SimpleBean.class.getMethod("setTimestamp", long.class);
		final PropertyGetter pg = LambdaAccessorFactory.createGetter(getter);
		final PropertySetter ps = LambdaAccessorFactory.createSetter(setter);

		final SimpleBean bean = new SimpleBean();
		ps.set(bean, 1234567890L);
		Assertions.assertEquals(1234567890L, pg.get(bean));
	}

	@Test
	void testGetterSetterPrimitiveInt() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("getCount");
		final Method setter = SimpleBean.class.getMethod("setCount", int.class);
		final PropertyGetter pg = LambdaAccessorFactory.createGetter(getter);
		final PropertySetter ps = LambdaAccessorFactory.createSetter(setter);

		final SimpleBean bean = new SimpleBean();
		ps.set(bean, 42);
		Assertions.assertEquals(42, pg.get(bean));
	}

	@Test
	void testGetterSetterPrimitiveDouble() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("getRatio");
		final Method setter = SimpleBean.class.getMethod("setRatio", double.class);
		final PropertyGetter pg = LambdaAccessorFactory.createGetter(getter);
		final PropertySetter ps = LambdaAccessorFactory.createSetter(setter);

		final SimpleBean bean = new SimpleBean();
		ps.set(bean, 3.14);
		Assertions.assertEquals(3.14, pg.get(bean));
	}

	@Test
	void testGetterSetterPrimitiveFloat() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("getScore");
		final Method setter = SimpleBean.class.getMethod("setScore", float.class);
		final PropertyGetter pg = LambdaAccessorFactory.createGetter(getter);
		final PropertySetter ps = LambdaAccessorFactory.createSetter(setter);

		final SimpleBean bean = new SimpleBean();
		ps.set(bean, 2.5f);
		Assertions.assertEquals(2.5f, pg.get(bean));
	}

	@Test
	void testGetterSetterPrimitiveShort() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("getLevel");
		final Method setter = SimpleBean.class.getMethod("setLevel", short.class);
		final PropertyGetter pg = LambdaAccessorFactory.createGetter(getter);
		final PropertySetter ps = LambdaAccessorFactory.createSetter(setter);

		final SimpleBean bean = new SimpleBean();
		ps.set(bean, (short) 7);
		Assertions.assertEquals((short) 7, pg.get(bean));
	}

	@Test
	void testGetterSetterPrimitiveByte() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("getFlags");
		final Method setter = SimpleBean.class.getMethod("setFlags", byte.class);
		final PropertyGetter pg = LambdaAccessorFactory.createGetter(getter);
		final PropertySetter ps = LambdaAccessorFactory.createSetter(setter);

		final SimpleBean bean = new SimpleBean();
		ps.set(bean, (byte) 0x1F);
		Assertions.assertEquals((byte) 0x1F, pg.get(bean));
	}

	@Test
	void testGetterSetterPrimitiveChar() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("getInitial");
		final Method setter = SimpleBean.class.getMethod("setInitial", char.class);
		final PropertyGetter pg = LambdaAccessorFactory.createGetter(getter);
		final PropertySetter ps = LambdaAccessorFactory.createSetter(setter);

		final SimpleBean bean = new SimpleBean();
		ps.set(bean, 'Z');
		Assertions.assertEquals('Z', pg.get(bean));
	}

	// ===== Typed primitive method-based getter/setter =====

	@Test
	void testTypedGetterSetterPrimitiveLong() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("getTimestamp");
		final Method setter = SimpleBean.class.getMethod("setTimestamp", long.class);
		final TypedPropertyGetter<SimpleBean, Long> tg = LambdaAccessorFactory.createTypedGetter(getter,
				SimpleBean.class, Long.class);
		final TypedPropertySetter<SimpleBean, Long> ts = LambdaAccessorFactory.createTypedSetter(setter,
				SimpleBean.class, Long.class);

		final SimpleBean bean = new SimpleBean();
		ts.set(bean, 9876543210L);
		Assertions.assertEquals(9876543210L, tg.get(bean));
	}

	@Test
	void testTypedGetterSetterPrimitiveInt() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("getCount");
		final Method setter = SimpleBean.class.getMethod("setCount", int.class);
		final TypedPropertyGetter<SimpleBean, Integer> tg = LambdaAccessorFactory.createTypedGetter(getter,
				SimpleBean.class, Integer.class);
		final TypedPropertySetter<SimpleBean, Integer> ts = LambdaAccessorFactory.createTypedSetter(setter,
				SimpleBean.class, Integer.class);

		final SimpleBean bean = new SimpleBean();
		ts.set(bean, 123);
		Assertions.assertEquals(123, tg.get(bean));
	}

	@Test
	void testTypedGetterSetterPrimitiveDouble() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("getRatio");
		final Method setter = SimpleBean.class.getMethod("setRatio", double.class);
		final TypedPropertyGetter<SimpleBean, Double> tg = LambdaAccessorFactory.createTypedGetter(getter,
				SimpleBean.class, Double.class);
		final TypedPropertySetter<SimpleBean, Double> ts = LambdaAccessorFactory.createTypedSetter(setter,
				SimpleBean.class, Double.class);

		final SimpleBean bean = new SimpleBean();
		ts.set(bean, 2.718);
		Assertions.assertEquals(2.718, tg.get(bean));
	}

	@Test
	void testTypedGetterSetterPrimitiveBoolean() throws Throwable {
		final Method getter = SimpleBean.class.getMethod("isActive");
		final Method setter = SimpleBean.class.getMethod("setActive", boolean.class);
		final TypedPropertyGetter<SimpleBean, Boolean> tg = LambdaAccessorFactory.createTypedGetter(getter,
				SimpleBean.class, Boolean.class);
		final TypedPropertySetter<SimpleBean, Boolean> ts = LambdaAccessorFactory.createTypedSetter(setter,
				SimpleBean.class, Boolean.class);

		final SimpleBean bean = new SimpleBean();
		ts.set(bean, true);
		Assertions.assertTrue(tg.get(bean));
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
		final TypedPropertyGetter<SimpleBean, String> tg = LambdaAccessorFactory.createTypedGetter(getter,
				SimpleBean.class, String.class);

		final SimpleBean bean = new SimpleBean();
		bean.setTitle("Typed");
		final String result = tg.get(bean);
		Assertions.assertEquals("Typed", result);
	}

	@Test
	void testCreateTypedSetterFromMethod() throws Throwable {
		final Method setter = SimpleBean.class.getMethod("setTitle", String.class);
		final TypedPropertySetter<SimpleBean, String> ts = LambdaAccessorFactory.createTypedSetter(setter,
				SimpleBean.class, String.class);

		final SimpleBean bean = new SimpleBean();
		ts.set(bean, "TypedSet");
		Assertions.assertEquals("TypedSet", bean.getTitle());
	}

	@Test
	void testCreateTypedFieldGetter() throws Throwable {
		final Field field = SimpleBean.class.getField("name");
		final TypedPropertyGetter<SimpleBean, String> tg = LambdaAccessorFactory.createTypedFieldGetter(field,
				SimpleBean.class, String.class);

		final SimpleBean bean = new SimpleBean();
		bean.name = "TypedField";
		Assertions.assertEquals("TypedField", tg.get(bean));
	}

	@Test
	void testCreateTypedFieldSetter() throws Throwable {
		final Field field = SimpleBean.class.getField("name");
		final TypedPropertySetter<SimpleBean, String> ts = LambdaAccessorFactory.createTypedFieldSetter(field,
				SimpleBean.class, String.class);
		Assertions.assertNotNull(ts);

		final SimpleBean bean = new SimpleBean();
		ts.set(bean, "TypedFieldSet");
		Assertions.assertEquals("TypedFieldSet", bean.name);
	}

	@Test
	void testCreateTypedFieldSetterFinalReturnsNull() throws Exception {
		final Field field = SimpleBean.class.getField("immutable");
		final TypedPropertySetter<SimpleBean, String> ts = LambdaAccessorFactory.createTypedFieldSetter(field,
				SimpleBean.class, String.class);
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
