package test.atriasoft.archidata.bean;

import org.atriasoft.archidata.bean.ClassModel;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Id;

/**
 * Unit tests for fluent setter detection in {@link ClassModel}.
 * Verifies that ClassModel correctly detects fluent setters (returning the entity)
 * and prioritizes void setters when both exist.
 */
public class TestClassModelFluentSetter {

	// ====================================================================
	// Test models
	// ====================================================================

	/** Model with only fluent setters (no void setters). */
	public static class FluentOnlyModel {
		@Id
		public String _id;
		public String name;
		public int age;

		public String getName() {
			return this.name;
		}

		public FluentOnlyModel setName(final String name) {
			this.name = name;
			return this;
		}

		public int getAge() {
			return this.age;
		}

		public FluentOnlyModel setAge(final int age) {
			this.age = age;
			return this;
		}
	}

	/** Model with both void and fluent setters for the same property. */
	public static class MixedSetterModel {
		@Id
		public String _id;
		public String name;

		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}

	/** Subclass adding a fluent setter where superclass has void setter. */
	public static class SubModel extends MixedSetterModel {
		// Inherits void setName from MixedSetterModel
		// This class does not override setName
	}

	@BeforeEach
	public void clearCaches() {
		ClassModel.clearCache();
	}

	// ====================================================================
	// Fluent setter detection
	// ====================================================================

	@Test
	public void testFluentSetterDetection() throws IntrospectionException {
		final ClassModel model = ClassModel.of(FluentOnlyModel.class);
		final PropertyDescriptor nameProp = model.getProperty("name");
		Assertions.assertNotNull(nameProp, "name property should exist");
		Assertions.assertNotNull(nameProp.getSetter(), "name should have a setter method");
		Assertions.assertTrue(nameProp.canWrite(), "name should be writable");
	}

	@Test
	public void testFluentSetterPropertyAccess() throws Throwable, IntrospectionException {
		final ClassModel model = ClassModel.of(FluentOnlyModel.class);
		final PropertyDescriptor nameProp = model.getProperty("name");
		Assertions.assertNotNull(nameProp, "name property should exist");

		final FluentOnlyModel instance = new FluentOnlyModel();
		nameProp.setValue(instance, "test-value");
		Assertions.assertEquals("test-value", instance.name);
	}

	@Test
	public void testFluentSetterPrimitivePropertyAccess() throws Throwable, IntrospectionException {
		final ClassModel model = ClassModel.of(FluentOnlyModel.class);
		final PropertyDescriptor ageProp = model.getProperty("age");
		Assertions.assertNotNull(ageProp, "age property should exist");

		final FluentOnlyModel instance = new FluentOnlyModel();
		ageProp.setValue(instance, 42);
		Assertions.assertEquals(42, instance.age);
	}

	// ====================================================================
	// Void setter priority
	// ====================================================================

	@Test
	public void testVoidSetterPriority() throws IntrospectionException {
		final ClassModel model = ClassModel.of(MixedSetterModel.class);
		final PropertyDescriptor nameProp = model.getProperty("name");
		Assertions.assertNotNull(nameProp, "name property should exist");
		Assertions.assertNotNull(nameProp.getSetter(), "name should have a setter method");
		// The void setter should be used (return type == void)
		Assertions.assertEquals(void.class, nameProp.getSetter().getReturnType(),
				"void setter should take priority over fluent setter");
	}
}
