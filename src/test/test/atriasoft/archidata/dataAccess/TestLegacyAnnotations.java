package test.atriasoft.archidata.dataAccess;

import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.ws.rs.DefaultValue;

/**
 * An entity still annotated with JPA or with a JAX-RS {@code @DefaultValue} on a field is rejected when its
 * model is analysed, instead of silently changing collection, field name or initial value.
 */
public class TestLegacyAnnotations {

	@Table(name = "legacy_table")
	public static class LegacyTable {
		public ObjectId oid;
	}

	public static class LegacyColumn {
		public ObjectId oid;
		@Column(name = "legacy_name")
		public String name;
	}

	public static class LegacyDefaultValue {
		public ObjectId oid;
		@DefaultValue("'0'")
		public Integer count;
	}

	public static class LegacyGetter {
		public ObjectId oid;
		private String name;

		@Column(name = "legacy_name")
		public String getName() {
			return this.name;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}

	public static class LegacyParent {
		public ObjectId oid;
		@Column(name = "parent_name")
		public String parentName;
	}

	public static class LegacyChild extends LegacyParent {
		public String childName;
	}

	public static class Clean {
		public ObjectId oid;
		public String name;
	}

	private static String errorOf(final Class<?> clazz) {
		final IntrospectionException ex = Assertions.assertThrows(IntrospectionException.class,
				() -> DbClassModel.of(clazz));
		final DataAccessException cause = Assertions.assertInstanceOf(DataAccessException.class, ex.getCause());
		return cause.getMessage();
	}

	@Test
	public void testTableIsRejected() {
		final String message = errorOf(LegacyTable.class);
		Assertions.assertTrue(message.contains(LegacyTable.class.getName()), message);
		Assertions.assertTrue(message.contains("@jakarta.persistence.Table"), message);
		Assertions.assertTrue(message.contains("@Collection"), message);
	}

	@Test
	public void testColumnOnFieldIsRejected() {
		final String message = errorOf(LegacyColumn.class);
		Assertions.assertTrue(message.contains("field LegacyColumn.name"), message);
		Assertions.assertTrue(message.contains("@jakarta.persistence.Column"), message);
		Assertions.assertTrue(message.contains("@Field"), message);
	}

	@Test
	public void testDefaultValueOnFieldIsRejected() {
		final String message = errorOf(LegacyDefaultValue.class);
		Assertions.assertTrue(message.contains("field LegacyDefaultValue.count"), message);
		Assertions.assertTrue(message.contains("@jakarta.ws.rs.DefaultValue"), message);
		Assertions.assertTrue(message.contains("@InitValue"), message);
	}

	@Test
	public void testColumnOnGetterIsRejected() {
		final String message = errorOf(LegacyGetter.class);
		Assertions.assertTrue(message.contains("method LegacyGetter.getName()"), message);
	}

	@Test
	public void testInheritedColumnIsRejected() {
		final String message = errorOf(LegacyChild.class);
		Assertions.assertTrue(message.contains(LegacyChild.class.getName()), message);
		Assertions.assertTrue(message.contains("field LegacyParent.parentName"), message);
	}

	@Test
	public void testCleanEntityIsAccepted() throws Exception {
		Assertions.assertNotNull(DbClassModel.of(Clean.class));
	}
}
