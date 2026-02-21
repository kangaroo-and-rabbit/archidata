package test.atriasoft.archidata.bean;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.bean.annotation.UnifiedAnnotationLookup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UnifiedAnnotationLookup} — verifies annotation merging
 * across field, getter, setter, and interface declarations.
 */
class TestUnifiedAnnotationLookup {

	// ===== Annotations =====

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD, ElementType.METHOD })
	@interface Alpha {
		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD, ElementType.METHOD })
	@interface Beta {
		String value() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.FIELD, ElementType.METHOD })
	@interface Gamma {
		String value() default "";
	}

	// ===== Test model classes =====

	public static class FieldOnly {
		@Alpha("field")
		public String name;
	}

	public static class GetterOnly {
		private String name;

		@Alpha("getter")
		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}

	public static class SetterOnly {
		private String name;

		public String getName() {
			return this.name;
		}

		@Alpha("setter")
		public void setName(final String name) {
			this.name = name;
		}
	}

	public static class FieldAndGetter {
		@Alpha("field")
		private String name;

		@Alpha("getter")
		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}

	public static class MultipleAnnotations {
		@Alpha("field-alpha")
		@Beta("field-beta")
		private String name;

		@Gamma("getter-gamma")
		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}

	public interface AnnotatedInterface {
		@Alpha("interface")
		String getName();
	}

	public static class ImplementsInterface implements AnnotatedInterface {
		private String name;

		@Override
		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}

	public static class OverridesInterfaceAnnotation implements AnnotatedInterface {
		private String name;

		@Override
		@Alpha("class-getter")
		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}

	// ===== Tests =====

	@Test
	void testFieldAnnotation() throws Exception {
		final Field field = FieldOnly.class.getField("name");
		final Map<Class<? extends Annotation>, Annotation> cache = UnifiedAnnotationLookup.buildAnnotationCache(
				field, null, null);

		Assertions.assertTrue(UnifiedAnnotationLookup.has(cache, Alpha.class));
		Assertions.assertEquals("field", UnifiedAnnotationLookup.get(cache, Alpha.class).value());
	}

	@Test
	void testGetterAnnotation() throws Exception {
		final Method getter = GetterOnly.class.getMethod("getName");
		final Map<Class<? extends Annotation>, Annotation> cache = UnifiedAnnotationLookup.buildAnnotationCache(
				null, getter, null);

		Assertions.assertTrue(UnifiedAnnotationLookup.has(cache, Alpha.class));
		Assertions.assertEquals("getter", UnifiedAnnotationLookup.get(cache, Alpha.class).value());
	}

	@Test
	void testSetterAnnotation() throws Exception {
		final Method setter = SetterOnly.class.getMethod("setName", String.class);
		final Map<Class<? extends Annotation>, Annotation> cache = UnifiedAnnotationLookup.buildAnnotationCache(
				null, null, setter);

		Assertions.assertTrue(UnifiedAnnotationLookup.has(cache, Alpha.class));
		Assertions.assertEquals("setter", UnifiedAnnotationLookup.get(cache, Alpha.class).value());
	}

	@Test
	void testFieldPriorityOverGetter() throws Exception {
		final Field field = FieldAndGetter.class.getDeclaredField("name");
		final Method getter = FieldAndGetter.class.getMethod("getName");
		final Map<Class<? extends Annotation>, Annotation> cache = UnifiedAnnotationLookup.buildAnnotationCache(
				field, getter, null);

		// Field should win (higher priority)
		Assertions.assertTrue(UnifiedAnnotationLookup.has(cache, Alpha.class));
		Assertions.assertEquals("field", UnifiedAnnotationLookup.get(cache, Alpha.class).value());
	}

	@Test
	void testMultipleAnnotationTypes() throws Exception {
		final Field field = MultipleAnnotations.class.getDeclaredField("name");
		final Method getter = MultipleAnnotations.class.getMethod("getName");
		final Map<Class<? extends Annotation>, Annotation> cache = UnifiedAnnotationLookup.buildAnnotationCache(
				field, getter, null);

		Assertions.assertTrue(UnifiedAnnotationLookup.has(cache, Alpha.class));
		Assertions.assertTrue(UnifiedAnnotationLookup.has(cache, Beta.class));
		Assertions.assertTrue(UnifiedAnnotationLookup.has(cache, Gamma.class));
		Assertions.assertEquals("field-alpha", UnifiedAnnotationLookup.get(cache, Alpha.class).value());
		Assertions.assertEquals("field-beta", UnifiedAnnotationLookup.get(cache, Beta.class).value());
		Assertions.assertEquals("getter-gamma", UnifiedAnnotationLookup.get(cache, Gamma.class).value());
	}

	@Test
	void testInterfaceAnnotationInheritance() throws Exception {
		final Method getter = ImplementsInterface.class.getMethod("getName");
		final Map<Class<? extends Annotation>, Annotation> cache = UnifiedAnnotationLookup.buildAnnotationCache(
				null, getter, null);

		// Interface annotation should be picked up
		Assertions.assertTrue(UnifiedAnnotationLookup.has(cache, Alpha.class));
		Assertions.assertEquals("interface", UnifiedAnnotationLookup.get(cache, Alpha.class).value());
	}

	@Test
	void testClassAnnotationOverridesInterface() throws Exception {
		final Method getter = OverridesInterfaceAnnotation.class.getMethod("getName");
		final Map<Class<? extends Annotation>, Annotation> cache = UnifiedAnnotationLookup.buildAnnotationCache(
				null, getter, null);

		// Class getter annotation should have higher priority than interface
		Assertions.assertTrue(UnifiedAnnotationLookup.has(cache, Alpha.class));
		Assertions.assertEquals("class-getter", UnifiedAnnotationLookup.get(cache, Alpha.class).value());
	}

	@Test
	void testEmptyCache() {
		final Map<Class<? extends Annotation>, Annotation> cache = UnifiedAnnotationLookup.buildAnnotationCache(
				null, null, null);

		Assertions.assertTrue(cache.isEmpty());
		Assertions.assertFalse(UnifiedAnnotationLookup.has(cache, Alpha.class));
		Assertions.assertNull(UnifiedAnnotationLookup.get(cache, Alpha.class));
	}

	@Test
	void testGetAllFromMultipleSources() throws Exception {
		final Field field = MultipleAnnotations.class.getDeclaredField("name");
		final Method getter = MultipleAnnotations.class.getMethod("getName");

		// getAll should return all instances of Alpha (field has one)
		final List<Alpha> allAlpha = UnifiedAnnotationLookup.getAll(field, getter, null, Alpha.class);
		Assertions.assertEquals(1, allAlpha.size());
		Assertions.assertEquals("field-alpha", allAlpha.get(0).value());

		// getAll for Gamma (only on getter)
		final List<Gamma> allGamma = UnifiedAnnotationLookup.getAll(field, getter, null, Gamma.class);
		Assertions.assertEquals(1, allGamma.size());
		Assertions.assertEquals("getter-gamma", allGamma.get(0).value());
	}

	@Test
	void testCacheIsUnmodifiable() throws Exception {
		final Field field = FieldOnly.class.getField("name");
		final Map<Class<? extends Annotation>, Annotation> cache = UnifiedAnnotationLookup.buildAnnotationCache(
				field, null, null);

		Assertions.assertThrows(UnsupportedOperationException.class, () -> cache.put(Beta.class, null));
	}
}
