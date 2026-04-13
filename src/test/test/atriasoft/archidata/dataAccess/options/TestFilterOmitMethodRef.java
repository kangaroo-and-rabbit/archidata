package test.atriasoft.archidata.dataAccess.options;

import java.util.List;

import org.atriasoft.archidata.bean.ClassModel;
import org.atriasoft.archidata.dataAccess.MethodReferenceResolver;
import org.atriasoft.archidata.dataAccess.SerializableBiConsumer;
import org.atriasoft.archidata.dataAccess.FieldRef;
import org.atriasoft.archidata.dataAccess.SerializableBiFunction;
import org.atriasoft.archidata.dataAccess.SerializableFunction;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.options.FilterOmit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

/**
 * Unit tests for method reference constructors in {@link FilterOmit}.
 * These tests do NOT require a running MongoDB instance.
 */
public class TestFilterOmitMethodRef {

	public static class Model {
		@Id
		public String _id;
		public String name;
		public int age;
		@Column(name = "full_name")
		public String fullName;

		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public int getAge() {
			return this.age;
		}

		public void setAge(final int age) {
			this.age = age;
		}

		public String getFullName() {
			return this.fullName;
		}

		public void setFullName(final String fullName) {
			this.fullName = fullName;
		}
	}

	@BeforeEach
	public void clearCaches() {
		DbClassModel.clearCache();
		ClassModel.clearCache();
		MethodReferenceResolver.clearCache();
	}

	// ====================================================================
	// Getter reference constructors
	// ====================================================================

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor_getterRefs() {
		final FilterOmit fo = new FilterOmit((SerializableFunction<Model, String>) Model::getName,
				(SerializableFunction<Model, Integer>) Model::getAge);
		Assertions.assertEquals(List.of("name", "age"), fo.getValues());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor_getterRefs_withColumnRename() {
		final FilterOmit fo = new FilterOmit((SerializableFunction<Model, String>) Model::getFullName);
		Assertions.assertEquals(List.of("full_name"), fo.getValues());
	}

	// ====================================================================
	// Setter reference constructors
	// ====================================================================

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor_setterRefs() {
		final FilterOmit fo = new FilterOmit((SerializableBiConsumer<Model, String>) Model::setName,
				(SerializableBiConsumer<Model, Integer>) Model::setAge);
		Assertions.assertEquals(List.of("name", "age"), fo.getValues());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor_setterRefs_withColumnRename() {
		final FilterOmit fo = new FilterOmit((SerializableBiConsumer<Model, String>) Model::setFullName);
		Assertions.assertEquals(List.of("full_name"), fo.getValues());
	}

	// ====================================================================
	// Consistency with string constructor
	// ====================================================================

	@SuppressWarnings("unchecked")
	@Test
	public void testGetValues_matchesStringConstructor() {
		final FilterOmit fromStrings = new FilterOmit("name", "age");
		final FilterOmit fromGetters = new FilterOmit((SerializableFunction<Model, String>) Model::getName,
				(SerializableFunction<Model, Integer>) Model::getAge);
		Assertions.assertEquals(fromStrings.getValues(), fromGetters.getValues());
	}

	// ====================================================================
	// Fluent setter model
	// ====================================================================

	public static class FluentModel {
		@Id
		public String _id;
		public String name;
		public int age;
		@Column(name = "full_name")
		public String fullName;

		public String getName() {
			return this.name;
		}

		public FluentModel setName(final String name) {
			this.name = name;
			return this;
		}

		public int getAge() {
			return this.age;
		}

		public FluentModel setAge(final int age) {
			this.age = age;
			return this;
		}

		public String getFullName() {
			return this.fullName;
		}

		public FluentModel setFullName(final String fullName) {
			this.fullName = fullName;
			return this;
		}
	}

	// ====================================================================
	// Fluent setter reference constructors
	// ====================================================================

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor_fluentSetterRefs() {
		final FilterOmit fo = new FilterOmit((SerializableBiFunction<FluentModel, String, ?>) FluentModel::setName,
				(SerializableBiFunction<FluentModel, Integer, ?>) FluentModel::setAge);
		Assertions.assertEquals(List.of("name", "age"), fo.getValues());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor_fluentSetterRefs_withColumnRename() {
		final FilterOmit fo = new FilterOmit((SerializableBiFunction<FluentModel, String, ?>) FluentModel::setFullName);
		Assertions.assertEquals(List.of("full_name"), fo.getValues());
	}

	// ====================================================================
	// FieldRef mixed constructors
	// ====================================================================

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor_fieldRef_mixed() {
		final FilterOmit fo = new FilterOmit(
				FieldRef.of((SerializableFunction<FluentModel, String>) FluentModel::getName),
				FieldRef.of((SerializableBiFunction<FluentModel, Integer, ?>) FluentModel::setAge));
		Assertions.assertEquals(List.of("name", "age"), fo.getValues());
	}
}
