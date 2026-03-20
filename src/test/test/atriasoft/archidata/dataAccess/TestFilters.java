package test.atriasoft.archidata.dataAccess;

import java.util.List;
import java.util.regex.Pattern;

import org.atriasoft.archidata.bean.ClassModel;
import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.MethodReferenceResolver;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.bson.BsonType;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Id;

/**
 * Comprehensive unit tests for the {@link Filters} class and its method reference resolution.
 * These tests do NOT require a running MongoDB instance.
 *
 * <p>Every public method of {@link Filters} is tested:
 * <ul>
 * <li>String-based overloads (delegation to MongoDB Filters)</li>
 * <li>Getter method reference overloads ({@code SerializableFunction})</li>
 * <li>Setter method reference overloads ({@code SerializableBiConsumer})</li>
 * </ul>
 */
public class TestFilters {

	// ====================================================================
	// Test model classes
	// ====================================================================

	/** Model with public fields (most common pattern in archidata). */
	public static class PublicFieldModel {
		@Id
		public String _id;
		public String name;
		public int age;
		public boolean active;
		public String email;
		public List<String> tags;

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

		public String getEmail() {
			return this.email;
		}

		public void setEmail(final String email) {
			this.email = email;
		}

		public List<String> getTags() {
			return this.tags;
		}

		public void setTags(final List<String> tags) {
			this.tags = tags;
		}
	}

	/** Model with private fields and getter/setter only. */
	public static class GetterSetterModel {
		@Id
		private String _id;
		private String firstName;
		private boolean enabled;

		public String get_id() {
			return this._id;
		}

		public String getFirstName() {
			return this.firstName;
		}

		public void setFirstName(final String firstName) {
			this.firstName = firstName;
		}

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(final boolean enabled) {
			this.enabled = enabled;
		}
	}

	/** Model with @Column(name) for custom field name mapping. */
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
	}

	@BeforeEach
	public void clearCaches() {
		DbClassModel.clearCache();
		ClassModel.clearCache();
		MethodReferenceResolver.clearCache();
	}

	// ====================================================================
	// extractPropertyName tests
	// ====================================================================

	@Test
	public void testExtractPropertyName_getter() {
		Assertions.assertEquals("name", MethodReferenceResolver.extractPropertyName("getName"));
		Assertions.assertEquals("firstName", MethodReferenceResolver.extractPropertyName("getFirstName"));
		Assertions.assertEquals("age", MethodReferenceResolver.extractPropertyName("getAge"));
	}

	@Test
	public void testExtractPropertyName_booleanGetter() {
		Assertions.assertEquals("active", MethodReferenceResolver.extractPropertyName("isActive"));
		Assertions.assertEquals("enabled", MethodReferenceResolver.extractPropertyName("isEnabled"));
	}

	@Test
	public void testExtractPropertyName_setter() {
		Assertions.assertEquals("name", MethodReferenceResolver.extractPropertyName("setName"));
		Assertions.assertEquals("firstName", MethodReferenceResolver.extractPropertyName("setFirstName"));
	}

	@Test
	public void testExtractPropertyName_consecutiveUppercase() {
		// "URL" should stay as "URL", not "uRL"
		Assertions.assertEquals("URL", MethodReferenceResolver.extractPropertyName("getURL"));
		Assertions.assertEquals("URL", MethodReferenceResolver.extractPropertyName("setURL"));
	}

	@Test
	public void testExtractPropertyName_recordAccessor() {
		// Direct name (no get/is/set prefix) should be returned as-is
		Assertions.assertEquals("name", MethodReferenceResolver.extractPropertyName("name"));
		Assertions.assertEquals("age", MethodReferenceResolver.extractPropertyName("age"));
	}

	// ====================================================================
	// resolveFieldName tests with getter
	// ====================================================================

	@Test
	public void testResolveFieldName_getter() {
		final String fieldName = MethodReferenceResolver
				.resolveFieldName((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName);
		Assertions.assertEquals("name", fieldName);
	}

	@Test
	public void testResolveFieldName_booleanGetter() {
		final String fieldName = MethodReferenceResolver
				.resolveFieldName((SerializableGetter<PublicFieldModel, Boolean>) PublicFieldModel::isActive);
		Assertions.assertEquals("active", fieldName);
	}

	@Test
	public void testResolveFieldName_getterWithColumnAnnotation() {
		final String fieldName = MethodReferenceResolver
				.resolveFieldName((SerializableGetter<ColumnRenameModel, String>) ColumnRenameModel::getFullName);
		Assertions.assertEquals("full_name", fieldName);
	}

	@Test
	public void testResolveFieldName_normalFieldWithoutColumnAnnotation() {
		final String fieldName = MethodReferenceResolver
				.resolveFieldName((SerializableGetter<ColumnRenameModel, String>) ColumnRenameModel::getNormalField);
		Assertions.assertEquals("normalField", fieldName);
	}

	@Test
	public void testResolveFieldName_getterSetterModel() {
		final String fieldName = MethodReferenceResolver
				.resolveFieldName((SerializableGetter<GetterSetterModel, String>) GetterSetterModel::getFirstName);
		Assertions.assertEquals("firstName", fieldName);
	}

	@Test
	public void testResolveFieldName_booleanGetterSetterModel() {
		final String fieldName = MethodReferenceResolver
				.resolveFieldName((SerializableGetter<GetterSetterModel, Boolean>) GetterSetterModel::isEnabled);
		Assertions.assertEquals("enabled", fieldName);
	}

	// ====================================================================
	// resolveFieldName tests with setter
	// ====================================================================

	@Test
	public void testResolveFieldName_setter() {
		final String fieldName = MethodReferenceResolver
				.resolveFieldName((SerializableSetter<PublicFieldModel, String>) PublicFieldModel::setName);
		Assertions.assertEquals("name", fieldName);
	}

	@Test
	public void testResolveFieldName_setterWithColumnAnnotation() {
		final String fieldName = MethodReferenceResolver
				.resolveFieldName((SerializableSetter<ColumnRenameModel, String>) ColumnRenameModel::setFullName);
		Assertions.assertEquals("full_name", fieldName);
	}

	// ====================================================================
	// Cache tests
	// ====================================================================

	@Test
	public void testResolveFieldName_cacheHit() {
		final String first = MethodReferenceResolver
				.resolveFieldName((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName);
		final String second = MethodReferenceResolver
				.resolveFieldName((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName);
		Assertions.assertEquals(first, second);
		Assertions.assertEquals("name", first);
	}

	// ====================================================================
	// Filters string-based — Comparison operators
	// ====================================================================

	@Test
	public void testFilters_stringBased_eqNoField() {
		final Bson result = Filters.eq("John");
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_stringBased_eq() {
		final Bson result = Filters.eq("name", "John");
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_stringBased_ne() {
		final Bson result = Filters.ne("age", 30);
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_stringBased_gt() {
		Assertions.assertNotNull(Filters.gt("age", 18));
	}

	@Test
	public void testFilters_stringBased_lt() {
		Assertions.assertNotNull(Filters.lt("age", 65));
	}

	@Test
	public void testFilters_stringBased_gte() {
		Assertions.assertNotNull(Filters.gte("age", 18));
	}

	@Test
	public void testFilters_stringBased_lte() {
		Assertions.assertNotNull(Filters.lte("age", 65));
	}

	@Test
	public void testFilters_stringBased_inVarargs() {
		Assertions.assertNotNull(Filters.in("role", "admin", "user"));
	}

	@Test
	public void testFilters_stringBased_inIterable() {
		Assertions.assertNotNull(Filters.in("role", List.of("admin", "user")));
	}

	@Test
	public void testFilters_stringBased_ninVarargs() {
		Assertions.assertNotNull(Filters.nin("role", "guest", "banned"));
	}

	@Test
	public void testFilters_stringBased_ninIterable() {
		Assertions.assertNotNull(Filters.nin("role", List.of("guest", "banned")));
	}

	// ====================================================================
	// Filters string-based — Logical operators
	// ====================================================================

	@Test
	public void testFilters_stringBased_andVarargs() {
		final Bson result = Filters.and(Filters.eq("a", 1), Filters.eq("b", 2));
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_stringBased_andIterable() {
		final Bson result = Filters.and(List.of(Filters.eq("a", 1), Filters.eq("b", 2)));
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_stringBased_orVarargs() {
		final Bson result = Filters.or(Filters.eq("a", 1), Filters.eq("b", 2));
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_stringBased_orIterable() {
		final Bson result = Filters.or(List.of(Filters.eq("a", 1), Filters.eq("b", 2)));
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_stringBased_not() {
		final Bson result = Filters.not(Filters.eq("a", 1));
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_stringBased_norVarargs() {
		final Bson result = Filters.nor(Filters.eq("a", 1), Filters.eq("b", 2));
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_stringBased_norIterable() {
		final Bson result = Filters.nor(List.of(Filters.eq("a", 1), Filters.eq("b", 2)));
		Assertions.assertNotNull(result);
	}

	// ====================================================================
	// Filters string-based — Element operators
	// ====================================================================

	@Test
	public void testFilters_stringBased_exists() {
		Assertions.assertNotNull(Filters.exists("field"));
	}

	@Test
	public void testFilters_stringBased_existsFalse() {
		Assertions.assertNotNull(Filters.exists("field", false));
	}

	@Test
	public void testFilters_stringBased_typeBsonType() {
		Assertions.assertNotNull(Filters.type("field", BsonType.STRING));
	}

	@Test
	public void testFilters_stringBased_typeString() {
		Assertions.assertNotNull(Filters.type("field", "string"));
	}

	// ====================================================================
	// Filters string-based — Evaluation operators
	// ====================================================================

	@Test
	public void testFilters_stringBased_mod() {
		Assertions.assertNotNull(Filters.mod("count", 3, 1));
	}

	@Test
	public void testFilters_stringBased_regexString() {
		Assertions.assertNotNull(Filters.regex("email", ".*@example.com"));
	}

	@Test
	public void testFilters_stringBased_regexStringWithOptions() {
		Assertions.assertNotNull(Filters.regex("email", ".*@example.com", "i"));
	}

	@Test
	public void testFilters_stringBased_regexPattern() {
		Assertions.assertNotNull(Filters.regex("email", Pattern.compile(".*@example\\.com", Pattern.CASE_INSENSITIVE)));
	}

	@Test
	public void testFilters_stringBased_text() {
		Assertions.assertNotNull(Filters.text("search term"));
	}

	@Test
	public void testFilters_stringBased_where() {
		Assertions.assertNotNull(Filters.where("this.a > 10"));
	}

	@Test
	public void testFilters_stringBased_expr() {
		Assertions.assertNotNull(Filters.expr("$a"));
	}

	// ====================================================================
	// Filters string-based — Array operators
	// ====================================================================

	@Test
	public void testFilters_stringBased_allVarargs() {
		Assertions.assertNotNull(Filters.all("tags", "java", "mongodb"));
	}

	@Test
	public void testFilters_stringBased_allIterable() {
		Assertions.assertNotNull(Filters.all("tags", List.of("java", "mongodb")));
	}

	@Test
	public void testFilters_stringBased_elemMatch() {
		Assertions.assertNotNull(Filters.elemMatch("items", Filters.gt("price", 10)));
	}

	@Test
	public void testFilters_stringBased_size() {
		Assertions.assertNotNull(Filters.size("tags", 3));
	}

	// ====================================================================
	// Filters string-based — Miscellaneous
	// ====================================================================

	@Test
	public void testFilters_stringBased_jsonSchema() {
		Assertions.assertNotNull(Filters.jsonSchema(Filters.eq("type", "object")));
	}

	@Test
	public void testFilters_stringBased_empty() {
		Assertions.assertNotNull(Filters.empty());
	}

	// ====================================================================
	// Filters getter method reference — Comparison operators
	// ====================================================================

	@Test
	public void testFilters_getterRef_eq() {
		final Bson result = Filters.eq((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName,
				"John");
		Assertions.assertEquals(Filters.eq("name", "John").toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_ne() {
		final Bson result = Filters.ne((SerializableGetter<PublicFieldModel, Integer>) PublicFieldModel::getAge, 30);
		Assertions.assertEquals(Filters.ne("age", 30).toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_gt() {
		final Bson result = Filters.gt((SerializableGetter<PublicFieldModel, Integer>) PublicFieldModel::getAge, 18);
		Assertions.assertEquals(Filters.gt("age", 18).toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_gte() {
		final Bson result = Filters.gte((SerializableGetter<PublicFieldModel, Integer>) PublicFieldModel::getAge, 21);
		Assertions.assertEquals(Filters.gte("age", 21).toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_lt() {
		final Bson result = Filters.lt((SerializableGetter<PublicFieldModel, Integer>) PublicFieldModel::getAge, 65);
		Assertions.assertEquals(Filters.lt("age", 65).toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_lte() {
		final Bson result = Filters.lte((SerializableGetter<PublicFieldModel, Integer>) PublicFieldModel::getAge, 64);
		Assertions.assertEquals(Filters.lte("age", 64).toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_booleanGetter() {
		final Bson result = Filters.eq((SerializableGetter<PublicFieldModel, Boolean>) PublicFieldModel::isActive,
				true);
		Assertions.assertEquals(Filters.eq("active", true).toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_columnRename() {
		final Bson result = Filters.eq((SerializableGetter<ColumnRenameModel, String>) ColumnRenameModel::getFullName,
				"John Doe");
		Assertions.assertEquals(Filters.eq("full_name", "John Doe").toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_inVarargs() {
		final Bson result = Filters.in((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName,
				"Alice", "Bob");
		Assertions.assertEquals(Filters.in("name", "Alice", "Bob").toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_inIterable() {
		final Bson result = Filters.in((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName,
				List.of("Alice", "Bob"));
		Assertions.assertEquals(Filters.in("name", List.of("Alice", "Bob")).toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_ninVarargs() {
		final Bson result = Filters.nin((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName, "Eve",
				"Mallory");
		Assertions.assertEquals(Filters.nin("name", "Eve", "Mallory").toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_ninIterable() {
		final Bson result = Filters.nin((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName,
				List.of("Eve", "Mallory"));
		Assertions.assertEquals(Filters.nin("name", List.of("Eve", "Mallory")).toString(), result.toString());
	}

	// ====================================================================
	// Filters getter method reference — Element operators
	// ====================================================================

	@Test
	public void testFilters_getterRef_exists() {
		final Bson result = Filters.exists((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName);
		Assertions.assertEquals(Filters.exists("name").toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_existsFalse() {
		final Bson result = Filters.exists((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName,
				false);
		Assertions.assertEquals(Filters.exists("name", false).toString(), result.toString());
	}

	// ====================================================================
	// Filters getter method reference — Evaluation operators
	// ====================================================================

	@Test
	public void testFilters_getterRef_regexString() {
		final Bson result = Filters.regex((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getEmail,
				".*@example.com");
		Assertions.assertEquals(Filters.regex("email", ".*@example.com").toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_regexStringWithOptions() {
		final Bson result = Filters.regex((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getEmail,
				".*@example.com", "i");
		Assertions.assertEquals(Filters.regex("email", ".*@example.com", "i").toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_regexPattern() {
		final Pattern pattern = Pattern.compile(".*@example\\.com", Pattern.CASE_INSENSITIVE);
		final Bson result = Filters.regex((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getEmail,
				pattern);
		Assertions.assertEquals(Filters.regex("email", pattern).toString(), result.toString());
	}

	// ====================================================================
	// Filters getter method reference — Array operators
	// ====================================================================

	@Test
	public void testFilters_getterRef_allIterable() {
		final Bson result = Filters.all((SerializableGetter<PublicFieldModel, List<String>>) PublicFieldModel::getTags,
				List.of("java", "mongodb"));
		Assertions.assertEquals(Filters.all("tags", List.of("java", "mongodb")).toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_size() {
		final Bson result = Filters.size((SerializableGetter<PublicFieldModel, List<String>>) PublicFieldModel::getTags,
				3);
		Assertions.assertEquals(Filters.size("tags", 3).toString(), result.toString());
	}

	@Test
	public void testFilters_getterRef_elemMatch() {
		final Bson innerFilter = Filters.gt("value", 10);
		final Bson result = Filters
				.elemMatch((SerializableGetter<PublicFieldModel, List<String>>) PublicFieldModel::getTags, innerFilter);
		// elemMatch toString() uses object identity, so just verify non-null
		Assertions.assertNotNull(result);
	}

	// ====================================================================
	// Filters setter method reference — Comparison operators
	// ====================================================================

	@Test
	public void testFilters_setterRef_eq() {
		final Bson result = Filters.eq((SerializableSetter<PublicFieldModel, String>) PublicFieldModel::setName,
				"John");
		Assertions.assertEquals(Filters.eq("name", "John").toString(), result.toString());
	}

	@Test
	public void testFilters_setterRef_ne() {
		final Bson result = Filters.ne((SerializableSetter<PublicFieldModel, Integer>) PublicFieldModel::setAge, 30);
		Assertions.assertEquals(Filters.ne("age", 30).toString(), result.toString());
	}

	@Test
	public void testFilters_setterRef_gt() {
		final Bson result = Filters.gt((SerializableSetter<PublicFieldModel, Integer>) PublicFieldModel::setAge, 18);
		Assertions.assertEquals(Filters.gt("age", 18).toString(), result.toString());
	}

	@Test
	public void testFilters_setterRef_gte() {
		final Bson result = Filters.gte((SerializableSetter<PublicFieldModel, Integer>) PublicFieldModel::setAge, 21);
		Assertions.assertEquals(Filters.gte("age", 21).toString(), result.toString());
	}

	@Test
	public void testFilters_setterRef_lt() {
		final Bson result = Filters.lt((SerializableSetter<PublicFieldModel, Integer>) PublicFieldModel::setAge, 65);
		Assertions.assertEquals(Filters.lt("age", 65).toString(), result.toString());
	}

	@Test
	public void testFilters_setterRef_lte() {
		final Bson result = Filters.lte((SerializableSetter<PublicFieldModel, Integer>) PublicFieldModel::setAge, 64);
		Assertions.assertEquals(Filters.lte("age", 64).toString(), result.toString());
	}

	@Test
	public void testFilters_setterRef_columnRename() {
		final Bson result = Filters.eq((SerializableSetter<ColumnRenameModel, String>) ColumnRenameModel::setFullName,
				"Jane Doe");
		Assertions.assertEquals(Filters.eq("full_name", "Jane Doe").toString(), result.toString());
	}

	// ====================================================================
	// Filters setter method reference — Element operators
	// ====================================================================

	@Test
	public void testFilters_setterRef_exists() {
		final Bson result = Filters.exists((SerializableSetter<PublicFieldModel, String>) PublicFieldModel::setName);
		Assertions.assertEquals(Filters.exists("name").toString(), result.toString());
	}

	@Test
	public void testFilters_setterRef_existsFalse() {
		final Bson result = Filters.exists((SerializableSetter<PublicFieldModel, String>) PublicFieldModel::setName,
				false);
		Assertions.assertEquals(Filters.exists("name", false).toString(), result.toString());
	}

	// ====================================================================
	// Complex / combined tests
	// ====================================================================

	@Test
	public void testFilters_methodRef_andCombination() {
		final Bson result = Filters.and(
				Filters.gt((SerializableGetter<PublicFieldModel, Integer>) PublicFieldModel::getAge, 18),
				Filters.eq((SerializableGetter<PublicFieldModel, Boolean>) PublicFieldModel::isActive, true));
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_methodRef_orCombination() {
		final Bson result = Filters.or(
				Filters.eq((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName, "Alice"),
				Filters.eq((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName, "Bob"));
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_methodRef_notCombination() {
		final Bson result = Filters
				.not(Filters.eq((SerializableGetter<PublicFieldModel, Boolean>) PublicFieldModel::isActive, false));
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_methodRef_mixedStringAndRef() {
		// Combine string-based and method reference filters
		final Bson result = Filters.and(
				Filters.gt((SerializableGetter<PublicFieldModel, Integer>) PublicFieldModel::getAge, 18),
				Filters.eq("role", "admin"));
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_methodRef_complexNested() {
		// Nested and/or with method references
		final Bson result = Filters.and(
				Filters.or(
						Filters.eq((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName, "Alice"),
						Filters.eq((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName, "Bob")),
				Filters.gt((SerializableGetter<PublicFieldModel, Integer>) PublicFieldModel::getAge, 18),
				Filters.eq((SerializableGetter<PublicFieldModel, Boolean>) PublicFieldModel::isActive, true));
		Assertions.assertNotNull(result);
	}

	@Test
	public void testFilters_methodRef_getterAndSetterSameField() {
		// Getter and setter for the same field should produce the same Bson
		final Bson getterResult = Filters.eq((SerializableGetter<PublicFieldModel, String>) PublicFieldModel::getName,
				"test");
		final Bson setterResult = Filters.eq((SerializableSetter<PublicFieldModel, String>) PublicFieldModel::setName,
				"test");
		Assertions.assertEquals(getterResult.toString(), setterResult.toString());
	}

	// ====================================================================
	// Error case tests
	// ====================================================================

	@Test
	public void testResolveFieldName_invalidReference() {
		// Method reference on a non-model class should fail
		Assertions.assertThrows(IllegalArgumentException.class, () -> {
			MethodReferenceResolver.resolveFieldName(
					(org.atriasoft.archidata.dataAccess.SerializableFunction<String, Integer>) String::length);
		});
	}

	// ====================================================================
	// Type aliases for readability
	// ====================================================================

	/** Alias for SerializableFunction to improve test readability. */
	private interface SerializableGetter<T, R> extends org.atriasoft.archidata.dataAccess.SerializableFunction<T, R> {}

	/** Alias for SerializableBiConsumer to improve test readability. */
	private interface SerializableSetter<T, V>
			extends org.atriasoft.archidata.dataAccess.SerializableBiConsumer<T, V> {}

	/** Alias for SerializableBiFunction to improve test readability. */
	private interface SerializableFluentSetter<T, V>
			extends org.atriasoft.archidata.dataAccess.SerializableBiFunction<T, V, Object> {}

	// ====================================================================
	// Model with fluent setters
	// ====================================================================

	/** Model with fluent setters (return this for chaining). */
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
	// Fluent setter method reference overloads
	// ====================================================================

	@Test
	public void testFluentSetter_eq() {
		final Bson expected = Filters.eq("name", "John");
		final Bson result = Filters.eq(
				(org.atriasoft.archidata.dataAccess.SerializableBiFunction<FluentModel, String, ?>) FluentModel::setName,
				"John");
		Assertions.assertEquals(expected.toString(), result.toString());
	}

	@Test
	public void testFluentSetter_ne() {
		final Bson expected = Filters.ne("name", "John");
		final Bson result = Filters.ne(
				(org.atriasoft.archidata.dataAccess.SerializableBiFunction<FluentModel, String, ?>) FluentModel::setName,
				"John");
		Assertions.assertEquals(expected.toString(), result.toString());
	}

	@Test
	public void testFluentSetter_gt() {
		final Bson expected = Filters.gt("age", 18);
		final Bson result = Filters.gt(
				(org.atriasoft.archidata.dataAccess.SerializableBiFunction<FluentModel, Integer, ?>) FluentModel::setAge,
				18);
		Assertions.assertEquals(expected.toString(), result.toString());
	}

	@Test
	public void testFluentSetter_gte() {
		final Bson expected = Filters.gte("age", 18);
		final Bson result = Filters.gte(
				(org.atriasoft.archidata.dataAccess.SerializableBiFunction<FluentModel, Integer, ?>) FluentModel::setAge,
				18);
		Assertions.assertEquals(expected.toString(), result.toString());
	}

	@Test
	public void testFluentSetter_lt() {
		final Bson expected = Filters.lt("age", 18);
		final Bson result = Filters.lt(
				(org.atriasoft.archidata.dataAccess.SerializableBiFunction<FluentModel, Integer, ?>) FluentModel::setAge,
				18);
		Assertions.assertEquals(expected.toString(), result.toString());
	}

	@Test
	public void testFluentSetter_lte() {
		final Bson expected = Filters.lte("age", 18);
		final Bson result = Filters.lte(
				(org.atriasoft.archidata.dataAccess.SerializableBiFunction<FluentModel, Integer, ?>) FluentModel::setAge,
				18);
		Assertions.assertEquals(expected.toString(), result.toString());
	}

	@Test
	public void testFluentSetter_exists() {
		final Bson expected = Filters.exists("name");
		final Bson result = Filters.exists(
				(org.atriasoft.archidata.dataAccess.SerializableBiFunction<FluentModel, String, ?>) FluentModel::setName);
		Assertions.assertEquals(expected.toString(), result.toString());
	}

	@Test
	public void testFluentSetter_existsWithBoolean() {
		final Bson expected = Filters.exists("name", false);
		final Bson result = Filters.exists(
				(org.atriasoft.archidata.dataAccess.SerializableBiFunction<FluentModel, String, ?>) FluentModel::setName,
				false);
		Assertions.assertEquals(expected.toString(), result.toString());
	}

	@Test
	public void testFluentSetter_columnRename() {
		final Bson expected = Filters.eq("full_name", "John Doe");
		final Bson result = Filters.eq(
				(org.atriasoft.archidata.dataAccess.SerializableBiFunction<FluentModel, String, ?>) FluentModel::setFullName,
				"John Doe");
		Assertions.assertEquals(expected.toString(), result.toString());
	}
}
