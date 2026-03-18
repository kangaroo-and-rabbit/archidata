package test.atriasoft.archidata.dataAccess;

import java.util.List;

import org.atriasoft.archidata.bean.ClassModel;
import org.atriasoft.archidata.dataAccess.Fields;
import org.atriasoft.archidata.dataAccess.MethodReferenceResolver;
import org.atriasoft.archidata.dataAccess.SerializableBiConsumer;
import org.atriasoft.archidata.dataAccess.SerializableFunction;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

/**
 * Unit tests for {@link Fields} utility class.
 * These tests do NOT require a running MongoDB instance.
 */
public class TestFields {

	// ====================================================================
	// Test model classes
	// ====================================================================

	public static class SimpleModel {
		@Id
		public String _id;
		public String name;
		public int age;
		public boolean active;

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

		public boolean isActive() {
			return this.active;
		}

		public void setActive(final boolean active) {
			this.active = active;
		}
	}

	public static class ColumnRenameModel {
		@Id
		public String _id;
		@Column(name = "full_name")
		public String fullName;
		public String normalField;

		public String getFullName() {
			return this.fullName;
		}

		public void setFullName(final String fullName) {
			this.fullName = fullName;
		}

		public String getNormalField() {
			return this.normalField;
		}

		public void setNormalField(final String normalField) {
			this.normalField = normalField;
		}
	}

	@BeforeEach
	public void clearCaches() {
		DbClassModel.clearCache();
		ClassModel.clearCache();
		MethodReferenceResolver.clearCache();
	}

	// ====================================================================
	// Fields.of() with getter
	// ====================================================================

	@Test
	public void testOf_getter() {
		final String fieldName = Fields.of((SerializableFunction<SimpleModel, String>) SimpleModel::getName);
		Assertions.assertEquals("name", fieldName);
	}

	@Test
	public void testOf_booleanGetter() {
		final String fieldName = Fields.of((SerializableFunction<SimpleModel, Boolean>) SimpleModel::isActive);
		Assertions.assertEquals("active", fieldName);
	}

	@Test
	public void testOf_columnRename() {
		final String fieldName = Fields.of((SerializableFunction<ColumnRenameModel, String>) ColumnRenameModel::getFullName);
		Assertions.assertEquals("full_name", fieldName);
	}

	@Test
	public void testOf_normalFieldWithoutColumnAnnotation() {
		final String fieldName = Fields.of((SerializableFunction<ColumnRenameModel, String>) ColumnRenameModel::getNormalField);
		Assertions.assertEquals("normalField", fieldName);
	}

	// ====================================================================
	// Fields.of() with setter
	// ====================================================================

	@Test
	public void testOf_setter() {
		final String fieldName = Fields.of((SerializableBiConsumer<SimpleModel, String>) SimpleModel::setName);
		Assertions.assertEquals("name", fieldName);
	}

	@Test
	public void testOf_setterColumnRename() {
		final String fieldName = Fields.of((SerializableBiConsumer<ColumnRenameModel, String>) ColumnRenameModel::setFullName);
		Assertions.assertEquals("full_name", fieldName);
	}

	// ====================================================================
	// Fields.list() with getters
	// ====================================================================

	@SuppressWarnings("unchecked")
	@Test
	public void testList_getters() {
		final List<String> names = Fields.list(
				(SerializableFunction<SimpleModel, String>) SimpleModel::getName,
				(SerializableFunction<SimpleModel, Integer>) SimpleModel::getAge);
		Assertions.assertEquals(List.of("name", "age"), names);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testList_withColumnRename() {
		final List<String> names = Fields.list(
				(SerializableFunction<ColumnRenameModel, String>) ColumnRenameModel::getFullName,
				(SerializableFunction<ColumnRenameModel, String>) ColumnRenameModel::getNormalField);
		Assertions.assertEquals(List.of("full_name", "normalField"), names);
	}

	// ====================================================================
	// Fields.list() with setters
	// ====================================================================

	@SuppressWarnings("unchecked")
	@Test
	public void testList_setters() {
		final List<String> names = Fields.list(
				(SerializableBiConsumer<SimpleModel, String>) SimpleModel::setName,
				(SerializableBiConsumer<SimpleModel, Integer>) SimpleModel::setAge);
		Assertions.assertEquals(List.of("name", "age"), names);
	}

	// ====================================================================
	// Consistency: of() and list() produce same results
	// ====================================================================

	@SuppressWarnings("unchecked")
	@Test
	public void testConsistency_ofAndList() {
		final String single = Fields.of((SerializableFunction<SimpleModel, String>) SimpleModel::getName);
		final List<String> list = Fields.list(
				(SerializableFunction<SimpleModel, String>) SimpleModel::getName);
		Assertions.assertEquals(single, list.get(0));
	}

	// ====================================================================
	// Error cases
	// ====================================================================

	@Test
	public void testOf_invalidReference() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			Fields.of((SerializableFunction<String, Integer>) String::length);
		});
	}
}
