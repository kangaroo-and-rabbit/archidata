package test.atriasoft.archidata.dataAccess.options;

import java.util.List;

import org.atriasoft.archidata.bean.ClassModel;
import org.atriasoft.archidata.dataAccess.MethodReferenceResolver;
import org.atriasoft.archidata.dataAccess.SerializableBiConsumer;
import org.atriasoft.archidata.dataAccess.FieldRef;
import org.atriasoft.archidata.dataAccess.SerializableBiFunction;
import org.atriasoft.archidata.dataAccess.SerializableFunction;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

/**
 * Unit tests for method reference constructors in {@link FilterValue}.
 * These tests do NOT require a running MongoDB instance.
 */
public class TestFilterValueMethodRef {

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
		final FilterValue fv = new FilterValue((SerializableFunction<Model, String>) Model::getName,
				(SerializableFunction<Model, Integer>) Model::getAge);
		Assertions.assertEquals(List.of("name", "age"), fv.getValues());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor_getterRefs_withColumnRename() {
		final FilterValue fv = new FilterValue((SerializableFunction<Model, String>) Model::getFullName);
		Assertions.assertEquals(List.of("full_name"), fv.getValues());
	}

	// ====================================================================
	// Setter reference constructors
	// ====================================================================

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor_setterRefs() {
		final FilterValue fv = new FilterValue((SerializableBiConsumer<Model, String>) Model::setName,
				(SerializableBiConsumer<Model, Integer>) Model::setAge);
		Assertions.assertEquals(List.of("name", "age"), fv.getValues());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor_setterRefs_withColumnRename() {
		final FilterValue fv = new FilterValue((SerializableBiConsumer<Model, String>) Model::setFullName);
		Assertions.assertEquals(List.of("full_name"), fv.getValues());
	}

	// ====================================================================
	// Consistency with string constructor
	// ====================================================================

	@SuppressWarnings("unchecked")
	@Test
	public void testGetValues_matchesStringConstructor() {
		final FilterValue fromStrings = new FilterValue("name", "age");
		final FilterValue fromGetters = new FilterValue((SerializableFunction<Model, String>) Model::getName,
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
		final FilterValue fv = new FilterValue((SerializableBiFunction<FluentModel, String, ?>) FluentModel::setName,
				(SerializableBiFunction<FluentModel, Integer, ?>) FluentModel::setAge);
		Assertions.assertEquals(List.of("name", "age"), fv.getValues());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor_fluentSetterRefs_withColumnRename() {
		final FilterValue fv = new FilterValue(
				(SerializableBiFunction<FluentModel, String, ?>) FluentModel::setFullName);
		Assertions.assertEquals(List.of("full_name"), fv.getValues());
	}

	// ====================================================================
	// FieldRef mixed constructors
	// ====================================================================

	@SuppressWarnings("unchecked")
	@Test
	public void testConstructor_fieldRef_mixed() {
		final FilterValue fv = new FilterValue(
				FieldRef.of((SerializableFunction<FluentModel, String>) FluentModel::getName),
				FieldRef.of((SerializableBiFunction<FluentModel, Integer, ?>) FluentModel::setAge));
		Assertions.assertEquals(List.of("name", "age"), fv.getValues());
	}
}
