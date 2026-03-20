package org.atriasoft.archidata.dataAccess;

import java.util.regex.Pattern;

import org.bson.BsonType;
import org.bson.conversions.Bson;

/**
 * Type-safe wrapper around MongoDB {@link com.mongodb.client.model.Filters} that adds
 * method reference support for resolving entity property names to database field names.
 *
 * <p>All string-based methods have the <strong>same signature</strong> as
 * {@code com.mongodb.client.model.Filters}, so migration is just an import change:
 * <pre>
 * // Before:
 * import com.mongodb.client.model.Filters;
 * // After:
 * import org.atriasoft.archidata.dataAccess.Filters;
 * </pre>
 *
 * <h2>Method reference usage (type-safe, resolves DB field names):</h2>
 * <pre>{@code
 * Filters.eq(User::getName, "John")
 * Filters.gt(User::getAge, 18)
 * Filters.eq(User::isActive, true)
 * Filters.eq(User::setName, "John")    // setter reference also works
 * Filters.in(User::getName, "a", "b")
 * }</pre>
 *
 * <p>Method reference overloads use {@link org.atriasoft.archidata.dataAccess.model.DbClassModel}
 * to resolve the property name to the correct database field name, respecting
 * {@code @Column(name = "...")} annotations. Results are cached for performance.
 *
 * @see org.atriasoft.archidata.dataAccess.options.Condition
 * @see com.mongodb.client.model.Filters
 */
public final class Filters {

	private Filters() {}

	// ====================================================================
	// Comparison operators — String-based (delegate to MongoDB Filters)
	// ====================================================================

	/**
	 * Creates a filter that matches all documents where the value of {@code _id}
	 * equals the specified value.
	 *
	 * <pre>{@code
	 * // Match document with _id equal to 42
	 * Bson filter = Filters.eq(42);
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param value the value to match against {@code _id}
	 * @return the equality filter
	 */
	public static <TItem> Bson eq(final TItem value) {
		return com.mongodb.client.model.Filters.eq(value);
	}

	/**
	 * Creates a filter that matches all documents where the value of the given
	 * field equals the specified value.
	 *
	 * <pre>{@code
	 * // Match documents where "name" equals "John"
	 * Bson filter = Filters.eq("name", "John");
	 *
	 * // Match documents where "age" equals 25
	 * Bson filter = Filters.eq("age", 25);
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param fieldName the field name
	 * @param value the value to match
	 * @return the equality filter
	 */
	public static <TItem> Bson eq(final String fieldName, final TItem value) {
		return com.mongodb.client.model.Filters.eq(fieldName, value);
	}

	/**
	 * Creates a filter that matches all documents where the value of the given
	 * field does not equal the specified value.
	 *
	 * <pre>{@code
	 * // Match documents where "status" is not "deleted"
	 * Bson filter = Filters.ne("status", "deleted");
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param fieldName the field name
	 * @param value the value to exclude
	 * @return the not-equal filter
	 */
	public static <TItem> Bson ne(final String fieldName, final TItem value) {
		return com.mongodb.client.model.Filters.ne(fieldName, value);
	}

	/**
	 * Creates a filter that matches all documents where the value of the given
	 * field is greater than the specified value.
	 *
	 * <pre>{@code
	 * // Match documents where "age" is greater than 18
	 * Bson filter = Filters.gt("age", 18);
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param fieldName the field name
	 * @param value the lower bound (exclusive)
	 * @return the greater-than filter
	 */
	public static <TItem> Bson gt(final String fieldName, final TItem value) {
		return com.mongodb.client.model.Filters.gt(fieldName, value);
	}

	/**
	 * Creates a filter that matches all documents where the value of the given
	 * field is less than the specified value.
	 *
	 * <pre>{@code
	 * // Match documents where "price" is less than 100
	 * Bson filter = Filters.lt("price", 100);
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param fieldName the field name
	 * @param value the upper bound (exclusive)
	 * @return the less-than filter
	 */
	public static <TItem> Bson lt(final String fieldName, final TItem value) {
		return com.mongodb.client.model.Filters.lt(fieldName, value);
	}

	/**
	 * Creates a filter that matches all documents where the value of the given
	 * field is greater than or equal to the specified value.
	 *
	 * <pre>{@code
	 * // Match documents where "score" is at least 50
	 * Bson filter = Filters.gte("score", 50);
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param fieldName the field name
	 * @param value the lower bound (inclusive)
	 * @return the greater-than-or-equal filter
	 */
	public static <TItem> Bson gte(final String fieldName, final TItem value) {
		return com.mongodb.client.model.Filters.gte(fieldName, value);
	}

	/**
	 * Creates a filter that matches all documents where the value of the given
	 * field is less than or equal to the specified value.
	 *
	 * <pre>{@code
	 * // Match documents where "quantity" is at most 10
	 * Bson filter = Filters.lte("quantity", 10);
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param fieldName the field name
	 * @param value the upper bound (inclusive)
	 * @return the less-than-or-equal filter
	 */
	public static <TItem> Bson lte(final String fieldName, final TItem value) {
		return com.mongodb.client.model.Filters.lte(fieldName, value);
	}

	/**
	 * Creates a filter that matches all documents where the value of the given
	 * field equals any value in the specified list (varargs).
	 *
	 * <pre>{@code
	 * // Match documents where "status" is "active" or "pending"
	 * Bson filter = Filters.in("status", "active", "pending");
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param fieldName the field name
	 * @param values the list of accepted values
	 * @return the in filter
	 */
	@SafeVarargs
	public static <TItem> Bson in(final String fieldName, final TItem... values) {
		return com.mongodb.client.model.Filters.in(fieldName, values);
	}

	/**
	 * Creates a filter that matches all documents where the value of the given
	 * field equals any value in the specified iterable.
	 *
	 * <pre>{@code
	 * // Match documents where "role" is in the allowed roles list
	 * List<String> roles = List.of("admin", "editor");
	 * Bson filter = Filters.in("role", roles);
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param fieldName the field name
	 * @param values the iterable of accepted values
	 * @return the in filter
	 */
	public static <TItem> Bson in(final String fieldName, final Iterable<TItem> values) {
		return com.mongodb.client.model.Filters.in(fieldName, values);
	}

	/**
	 * Creates a filter that matches all documents where the value of the given
	 * field does not equal any value in the specified list (varargs).
	 *
	 * <pre>{@code
	 * // Exclude documents where "type" is "spam" or "trash"
	 * Bson filter = Filters.nin("type", "spam", "trash");
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param fieldName the field name
	 * @param values the list of excluded values
	 * @return the not-in filter
	 */
	@SafeVarargs
	public static <TItem> Bson nin(final String fieldName, final TItem... values) {
		return com.mongodb.client.model.Filters.nin(fieldName, values);
	}

	/**
	 * Creates a filter that matches all documents where the value of the given
	 * field does not equal any value in the specified iterable.
	 *
	 * <pre>{@code
	 * // Exclude documents where "category" is in the blacklist
	 * List<String> blacklist = List.of("banned", "suspended");
	 * Bson filter = Filters.nin("category", blacklist);
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param fieldName the field name
	 * @param values the iterable of excluded values
	 * @return the not-in filter
	 */
	public static <TItem> Bson nin(final String fieldName, final Iterable<TItem> values) {
		return com.mongodb.client.model.Filters.nin(fieldName, values);
	}

	// ====================================================================
	// Logical operators
	// ====================================================================

	/**
	 * Creates a filter that performs a logical AND of the provided filters (iterable).
	 *
	 * <pre>{@code
	 * // Match active users older than 18
	 * List<Bson> conditions = List.of(
	 *     Filters.eq("active", true),
	 *     Filters.gt("age", 18)
	 * );
	 * Bson filter = Filters.and(conditions);
	 * }</pre>
	 *
	 * @param filters the iterable of filters to combine
	 * @return the AND filter
	 */
	public static Bson and(final Iterable<Bson> filters) {
		return com.mongodb.client.model.Filters.and(filters);
	}

	/**
	 * Creates a filter that performs a logical AND of the provided filters (varargs).
	 *
	 * <pre>{@code
	 * // Match active users older than 18
	 * Bson filter = Filters.and(
	 *     Filters.eq("active", true),
	 *     Filters.gt("age", 18)
	 * );
	 * }</pre>
	 *
	 * @param filters the filters to combine
	 * @return the AND filter
	 */
	public static Bson and(final Bson... filters) {
		return com.mongodb.client.model.Filters.and(filters);
	}

	/**
	 * Creates a filter that performs a logical OR of the provided filters (iterable).
	 *
	 * <pre>{@code
	 * // Match documents that are either admin or have a high score
	 * List<Bson> conditions = List.of(
	 *     Filters.eq("role", "admin"),
	 *     Filters.gt("score", 90)
	 * );
	 * Bson filter = Filters.or(conditions);
	 * }</pre>
	 *
	 * @param filters the iterable of filters to combine
	 * @return the OR filter
	 */
	public static Bson or(final Iterable<Bson> filters) {
		return com.mongodb.client.model.Filters.or(filters);
	}

	/**
	 * Creates a filter that performs a logical OR of the provided filters (varargs).
	 *
	 * <pre>{@code
	 * // Match documents that are either admin or have a high score
	 * Bson filter = Filters.or(
	 *     Filters.eq("role", "admin"),
	 *     Filters.gt("score", 90)
	 * );
	 * }</pre>
	 *
	 * @param filters the filters to combine
	 * @return the OR filter
	 */
	public static Bson or(final Bson... filters) {
		return com.mongodb.client.model.Filters.or(filters);
	}

	/**
	 * Creates a filter that performs a logical NOT of the provided filter.
	 *
	 * <pre>{@code
	 * // Match documents where "status" is NOT "deleted"
	 * Bson filter = Filters.not(Filters.eq("status", "deleted"));
	 * }</pre>
	 *
	 * @param filter the filter to negate
	 * @return the NOT filter
	 */
	public static Bson not(final Bson filter) {
		return com.mongodb.client.model.Filters.not(filter);
	}

	/**
	 * Creates a filter that performs a logical NOR of the provided filters (varargs).
	 * Matches documents that fail all the specified conditions.
	 *
	 * <pre>{@code
	 * // Match documents that are neither deleted nor archived
	 * Bson filter = Filters.nor(
	 *     Filters.eq("status", "deleted"),
	 *     Filters.eq("status", "archived")
	 * );
	 * }</pre>
	 *
	 * @param filters the filters to NOR together
	 * @return the NOR filter
	 */
	public static Bson nor(final Bson... filters) {
		return com.mongodb.client.model.Filters.nor(filters);
	}

	/**
	 * Creates a filter that performs a logical NOR of the provided filters (iterable).
	 * Matches documents that fail all the specified conditions.
	 *
	 * <pre>{@code
	 * // Match documents that are neither deleted nor archived
	 * List<Bson> excludes = List.of(
	 *     Filters.eq("status", "deleted"),
	 *     Filters.eq("status", "archived")
	 * );
	 * Bson filter = Filters.nor(excludes);
	 * }</pre>
	 *
	 * @param filters the iterable of filters to NOR together
	 * @return the NOR filter
	 */
	public static Bson nor(final Iterable<Bson> filters) {
		return com.mongodb.client.model.Filters.nor(filters);
	}

	// ====================================================================
	// Element operators
	// ====================================================================

	/**
	 * Creates a filter that matches all documents that contain the given field.
	 *
	 * <pre>{@code
	 * // Match documents that have an "email" field
	 * Bson filter = Filters.exists("email");
	 * }</pre>
	 *
	 * @param fieldName the field name
	 * @return the exists filter
	 */
	public static Bson exists(final String fieldName) {
		return com.mongodb.client.model.Filters.exists(fieldName);
	}

	/**
	 * Creates a filter that matches documents based on whether the given field exists.
	 *
	 * <pre>{@code
	 * // Match documents that have an "email" field
	 * Bson filter = Filters.exists("email", true);
	 *
	 * // Match documents that do NOT have a "deletedAt" field
	 * Bson filter = Filters.exists("deletedAt", false);
	 * }</pre>
	 *
	 * @param fieldName the field name
	 * @param exists true to match documents with the field, false to match documents without
	 * @return the exists filter
	 */
	public static Bson exists(final String fieldName, final boolean exists) {
		return com.mongodb.client.model.Filters.exists(fieldName, exists);
	}

	/**
	 * Creates a filter that matches documents where the given field has the specified BSON type.
	 *
	 * <pre>{@code
	 * // Match documents where "age" is stored as a 32-bit integer
	 * Bson filter = Filters.type("age", BsonType.INT32);
	 * }</pre>
	 *
	 * @param fieldName the field name
	 * @param type the BSON type to match
	 * @return the type filter
	 */
	public static Bson type(final String fieldName, final BsonType type) {
		return com.mongodb.client.model.Filters.type(fieldName, type);
	}

	/**
	 * Creates a filter that matches documents where the given field has the specified type string.
	 *
	 * <pre>{@code
	 * // Match documents where "value" is a string
	 * Bson filter = Filters.type("value", "string");
	 *
	 * // Match documents where "count" is a number
	 * Bson filter = Filters.type("count", "number");
	 * }</pre>
	 *
	 * @param fieldName the field name
	 * @param type the type string (e.g. "string", "number", "array")
	 * @return the type filter
	 */
	public static Bson type(final String fieldName, final String type) {
		return com.mongodb.client.model.Filters.type(fieldName, type);
	}

	// ====================================================================
	// Evaluation operators
	// ====================================================================

	/**
	 * Creates a filter that matches documents where the value of a field divided
	 * by a divisor has the specified remainder (modulo operation).
	 *
	 * <pre>{@code
	 * // Match documents where "counter" is even (counter % 2 == 0)
	 * Bson filter = Filters.mod("counter", 2, 0);
	 *
	 * // Match documents where "id" % 3 == 1
	 * Bson filter = Filters.mod("id", 3, 1);
	 * }</pre>
	 *
	 * @param fieldName the field name
	 * @param divisor the divisor
	 * @param remainder the expected remainder
	 * @return the modulo filter
	 */
	public static Bson mod(final String fieldName, final long divisor, final long remainder) {
		return com.mongodb.client.model.Filters.mod(fieldName, divisor, remainder);
	}

	/**
	 * Creates a filter that matches documents where the value of the given field
	 * matches the specified regular expression pattern.
	 *
	 * <pre>{@code
	 * // Match documents where "email" contains "gmail"
	 * Bson filter = Filters.regex("email", "gmail");
	 *
	 * // Match documents where "name" starts with "Jo"
	 * Bson filter = Filters.regex("name", "^Jo");
	 * }</pre>
	 *
	 * @param fieldName the field name
	 * @param pattern the regex pattern string
	 * @return the regex filter
	 */
	public static Bson regex(final String fieldName, final String pattern) {
		return com.mongodb.client.model.Filters.regex(fieldName, pattern);
	}

	/**
	 * Creates a filter that matches documents where the value of the given field
	 * matches the specified regular expression pattern with options.
	 *
	 * <pre>{@code
	 * // Case-insensitive match for "john" in the "name" field
	 * Bson filter = Filters.regex("name", "john", "i");
	 *
	 * // Case-insensitive multiline match
	 * Bson filter = Filters.regex("bio", "^developer", "im");
	 * }</pre>
	 *
	 * @param fieldName the field name
	 * @param pattern the regex pattern string
	 * @param options the regex options (e.g. "i" for case-insensitive, "m" for multiline)
	 * @return the regex filter
	 */
	public static Bson regex(final String fieldName, final String pattern, final String options) {
		return com.mongodb.client.model.Filters.regex(fieldName, pattern, options);
	}

	/**
	 * Creates a filter that matches documents where the value of the given field
	 * matches the specified compiled {@link Pattern}.
	 *
	 * <pre>{@code
	 * // Match "name" field using a compiled pattern (case-insensitive)
	 * Pattern pattern = Pattern.compile("^john", Pattern.CASE_INSENSITIVE);
	 * Bson filter = Filters.regex("name", pattern);
	 * }</pre>
	 *
	 * @param fieldName the field name
	 * @param pattern the compiled regex pattern
	 * @return the regex filter
	 */
	public static Bson regex(final String fieldName, final Pattern pattern) {
		return com.mongodb.client.model.Filters.regex(fieldName, pattern);
	}

	/**
	 * Creates a filter that matches documents using a full-text search on text-indexed fields.
	 *
	 * <pre>{@code
	 * // Full-text search for "coffee shop"
	 * Bson filter = Filters.text("coffee shop");
	 * }</pre>
	 *
	 * @param search the search string
	 * @return the text filter
	 */
	public static Bson text(final String search) {
		return com.mongodb.client.model.Filters.text(search);
	}

	/**
	 * Creates a filter that matches documents using a JavaScript expression.
	 *
	 * <pre>{@code
	 * // Match documents where two fields are equal using JavaScript
	 * Bson filter = Filters.where("this.firstName === this.lastName");
	 * }</pre>
	 *
	 * @param javaScriptExpression the JavaScript expression
	 * @return the where filter
	 */
	public static Bson where(final String javaScriptExpression) {
		return com.mongodb.client.model.Filters.where(javaScriptExpression);
	}

	/**
	 * Creates a filter using an aggregation expression.
	 *
	 * <pre>{@code
	 * // Use an aggregation expression as a filter
	 * Bson filter = Filters.expr(
	 *     Document.parse("{ $gt: ['$spent', '$budget'] }")
	 * );
	 * }</pre>
	 *
	 * @param <TExpression> the expression type
	 * @param expression the aggregation expression
	 * @return the expression filter
	 */
	public static <TExpression> Bson expr(final TExpression expression) {
		return com.mongodb.client.model.Filters.expr(expression);
	}

	// ====================================================================
	// Array operators
	// ====================================================================

	/**
	 * Creates a filter that matches documents where an array field contains all
	 * the specified values (varargs).
	 *
	 * <pre>{@code
	 * // Match documents where "tags" contains both "java" and "mongodb"
	 * Bson filter = Filters.all("tags", "java", "mongodb");
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param fieldName the array field name
	 * @param values the values that must all be present
	 * @return the all filter
	 */
	@SafeVarargs
	public static <TItem> Bson all(final String fieldName, final TItem... values) {
		return com.mongodb.client.model.Filters.all(fieldName, values);
	}

	/**
	 * Creates a filter that matches documents where an array field contains all
	 * the specified values (iterable).
	 *
	 * <pre>{@code
	 * // Match documents where "permissions" contains all required permissions
	 * List<String> required = List.of("read", "write");
	 * Bson filter = Filters.all("permissions", required);
	 * }</pre>
	 *
	 * @param <TItem> the value type
	 * @param fieldName the array field name
	 * @param values the iterable of values that must all be present
	 * @return the all filter
	 */
	public static <TItem> Bson all(final String fieldName, final Iterable<TItem> values) {
		return com.mongodb.client.model.Filters.all(fieldName, values);
	}

	/**
	 * Creates a filter that matches documents where an array field contains at least
	 * one element matching the specified sub-filter.
	 *
	 * <pre>{@code
	 * // Match documents where "scores" array has at least one element > 90
	 * Bson filter = Filters.elemMatch("scores", Filters.gt("value", 90));
	 *
	 * // Match documents where "addresses" has a city equal to "Paris"
	 * Bson filter = Filters.elemMatch("addresses", Filters.eq("city", "Paris"));
	 * }</pre>
	 *
	 * @param fieldName the array field name
	 * @param filter the sub-filter to match array elements against
	 * @return the elemMatch filter
	 */
	public static Bson elemMatch(final String fieldName, final Bson filter) {
		return com.mongodb.client.model.Filters.elemMatch(fieldName, filter);
	}

	/**
	 * Creates a filter that matches documents where the array field has the
	 * specified number of elements.
	 *
	 * <pre>{@code
	 * // Match documents where "tags" array has exactly 3 elements
	 * Bson filter = Filters.size("tags", 3);
	 * }</pre>
	 *
	 * @param fieldName the array field name
	 * @param size the expected array size
	 * @return the size filter
	 */
	public static Bson size(final String fieldName, final int size) {
		return com.mongodb.client.model.Filters.size(fieldName, size);
	}

	// ====================================================================
	// Miscellaneous
	// ====================================================================

	/**
	 * Creates a filter that matches documents that comply with the specified JSON schema.
	 *
	 * <pre>{@code
	 * // Validate documents against a JSON schema
	 * Bson schema = Document.parse(
	 *     "{ required: ['name', 'email'], properties: { name: { bsonType: 'string' } } }"
	 * );
	 * Bson filter = Filters.jsonSchema(schema);
	 * }</pre>
	 *
	 * @param schema the JSON schema as a Bson document
	 * @return the JSON schema filter
	 */
	public static Bson jsonSchema(final Bson schema) {
		return com.mongodb.client.model.Filters.jsonSchema(schema);
	}

	/**
	 * Creates an empty filter that matches all documents.
	 *
	 * <pre>{@code
	 * // Match all documents (no filtering)
	 * Bson filter = Filters.empty();
	 * }</pre>
	 *
	 * @return an empty filter
	 */
	public static Bson empty() {
		return com.mongodb.client.model.Filters.empty();
	}

	// ====================================================================
	// Comparison operators — Getter method reference overloads
	// ====================================================================

	/**
	 * Creates an equality filter using a getter method reference.
	 * The field name is resolved from the method reference using
	 * {@link MethodReferenceResolver}.
	 *
	 * <pre>{@code
	 * // Match documents where the "name" field equals "John"
	 * Bson filter = Filters.eq(User::getName, "John");
	 *
	 * // Match documents where "active" is true
	 * Bson filter = Filters.eq(User::isActive, true);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the field value type
	 * @param getter the getter method reference
	 * @param value the value to match
	 * @return the equality filter
	 */
	public static <T, R> Bson eq(final SerializableFunction<T, R> getter, final R value) {
		return com.mongodb.client.model.Filters.eq(MethodReferenceResolver.resolveFieldName(getter), value);
	}

	/**
	 * Creates a not-equal filter using a getter method reference.
	 *
	 * <pre>{@code
	 * // Match documents where "status" is not "deleted"
	 * Bson filter = Filters.ne(User::getStatus, "deleted");
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the field value type
	 * @param getter the getter method reference
	 * @param value the value to exclude
	 * @return the not-equal filter
	 */
	public static <T, R> Bson ne(final SerializableFunction<T, R> getter, final R value) {
		return com.mongodb.client.model.Filters.ne(MethodReferenceResolver.resolveFieldName(getter), value);
	}

	/**
	 * Creates a greater-than filter using a getter method reference.
	 *
	 * <pre>{@code
	 * // Match documents where "age" is greater than 18
	 * Bson filter = Filters.gt(User::getAge, 18);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the field value type
	 * @param getter the getter method reference
	 * @param value the lower bound (exclusive)
	 * @return the greater-than filter
	 */
	public static <T, R> Bson gt(final SerializableFunction<T, R> getter, final R value) {
		return com.mongodb.client.model.Filters.gt(MethodReferenceResolver.resolveFieldName(getter), value);
	}

	/**
	 * Creates a greater-than-or-equal filter using a getter method reference.
	 *
	 * <pre>{@code
	 * // Match documents where "score" is at least 50
	 * Bson filter = Filters.gte(User::getScore, 50);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the field value type
	 * @param getter the getter method reference
	 * @param value the lower bound (inclusive)
	 * @return the greater-than-or-equal filter
	 */
	public static <T, R> Bson gte(final SerializableFunction<T, R> getter, final R value) {
		return com.mongodb.client.model.Filters.gte(MethodReferenceResolver.resolveFieldName(getter), value);
	}

	/**
	 * Creates a less-than filter using a getter method reference.
	 *
	 * <pre>{@code
	 * // Match documents where "price" is less than 100
	 * Bson filter = Filters.lt(Product::getPrice, 100.0);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the field value type
	 * @param getter the getter method reference
	 * @param value the upper bound (exclusive)
	 * @return the less-than filter
	 */
	public static <T, R> Bson lt(final SerializableFunction<T, R> getter, final R value) {
		return com.mongodb.client.model.Filters.lt(MethodReferenceResolver.resolveFieldName(getter), value);
	}

	/**
	 * Creates a less-than-or-equal filter using a getter method reference.
	 *
	 * <pre>{@code
	 * // Match documents where "quantity" is at most 10
	 * Bson filter = Filters.lte(Product::getQuantity, 10);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the field value type
	 * @param getter the getter method reference
	 * @param value the upper bound (inclusive)
	 * @return the less-than-or-equal filter
	 */
	public static <T, R> Bson lte(final SerializableFunction<T, R> getter, final R value) {
		return com.mongodb.client.model.Filters.lte(MethodReferenceResolver.resolveFieldName(getter), value);
	}

	/**
	 * Creates an in filter using a getter method reference (varargs).
	 *
	 * <pre>{@code
	 * // Match documents where "role" is "admin" or "editor"
	 * Bson filter = Filters.in(User::getRole, "admin", "editor");
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the field value type
	 * @param getter the getter method reference
	 * @param values the list of accepted values
	 * @return the in filter
	 */
	@SafeVarargs
	public static <T, R> Bson in(final SerializableFunction<T, R> getter, final R... values) {
		return com.mongodb.client.model.Filters.in(MethodReferenceResolver.resolveFieldName(getter), values);
	}

	/**
	 * Creates an in filter using a getter method reference (iterable).
	 *
	 * <pre>{@code
	 * // Match documents where "status" is in the allowed statuses
	 * List<String> statuses = List.of("active", "pending");
	 * Bson filter = Filters.in(User::getStatus, statuses);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the field value type
	 * @param getter the getter method reference
	 * @param values the iterable of accepted values
	 * @return the in filter
	 */
	public static <T, R> Bson in(final SerializableFunction<T, R> getter, final Iterable<R> values) {
		return com.mongodb.client.model.Filters.in(MethodReferenceResolver.resolveFieldName(getter), values);
	}

	/**
	 * Creates a not-in filter using a getter method reference (varargs).
	 *
	 * <pre>{@code
	 * // Exclude documents where "type" is "spam" or "trash"
	 * Bson filter = Filters.nin(Message::getType, "spam", "trash");
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the field value type
	 * @param getter the getter method reference
	 * @param values the list of excluded values
	 * @return the not-in filter
	 */
	@SafeVarargs
	public static <T, R> Bson nin(final SerializableFunction<T, R> getter, final R... values) {
		return com.mongodb.client.model.Filters.nin(MethodReferenceResolver.resolveFieldName(getter), values);
	}

	/**
	 * Creates a not-in filter using a getter method reference (iterable).
	 *
	 * <pre>{@code
	 * // Exclude documents where "category" is in the blacklist
	 * List<String> blacklist = List.of("banned", "suspended");
	 * Bson filter = Filters.nin(User::getCategory, blacklist);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the field value type
	 * @param getter the getter method reference
	 * @param values the iterable of excluded values
	 * @return the not-in filter
	 */
	public static <T, R> Bson nin(final SerializableFunction<T, R> getter, final Iterable<R> values) {
		return com.mongodb.client.model.Filters.nin(MethodReferenceResolver.resolveFieldName(getter), values);
	}

	// ====================================================================
	// Element operators — Getter method reference overloads
	// ====================================================================

	/**
	 * Creates a filter that matches documents containing the field resolved from
	 * the getter method reference.
	 *
	 * <pre>{@code
	 * // Match documents that have an "email" field
	 * Bson filter = Filters.exists(User::getEmail);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the field value type
	 * @param getter the getter method reference
	 * @return the exists filter
	 */
	public static <T, R> Bson exists(final SerializableFunction<T, R> getter) {
		return com.mongodb.client.model.Filters.exists(MethodReferenceResolver.resolveFieldName(getter));
	}

	/**
	 * Creates a filter that matches documents based on whether the field resolved
	 * from the getter method reference exists or not.
	 *
	 * <pre>{@code
	 * // Match documents that have a "phone" field
	 * Bson filter = Filters.exists(User::getPhone, true);
	 *
	 * // Match documents that do NOT have a "deletedAt" field
	 * Bson filter = Filters.exists(User::getDeletedAt, false);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the field value type
	 * @param getter the getter method reference
	 * @param exists true to match documents with the field, false without
	 * @return the exists filter
	 */
	public static <T, R> Bson exists(final SerializableFunction<T, R> getter, final boolean exists) {
		return com.mongodb.client.model.Filters.exists(MethodReferenceResolver.resolveFieldName(getter), exists);
	}

	// ====================================================================
	// Evaluation operators — Getter method reference overloads
	// ====================================================================

	/**
	 * Creates a regex filter using a getter method reference.
	 *
	 * <pre>{@code
	 * // Match documents where "email" contains "gmail"
	 * Bson filter = Filters.regex(User::getEmail, "gmail");
	 *
	 * // Match documents where "name" starts with "Jo"
	 * Bson filter = Filters.regex(User::getName, "^Jo");
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param getter the getter method reference (must return String)
	 * @param pattern the regex pattern string
	 * @return the regex filter
	 */
	public static <T> Bson regex(final SerializableFunction<T, String> getter, final String pattern) {
		return com.mongodb.client.model.Filters.regex(MethodReferenceResolver.resolveFieldName(getter), pattern);
	}

	/**
	 * Creates a regex filter using a getter method reference with options.
	 *
	 * <pre>{@code
	 * // Case-insensitive match for "john" in the "name" field
	 * Bson filter = Filters.regex(User::getName, "john", "i");
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param getter the getter method reference (must return String)
	 * @param pattern the regex pattern string
	 * @param options the regex options (e.g. "i" for case-insensitive)
	 * @return the regex filter
	 */
	public static <T> Bson regex(
			final SerializableFunction<T, String> getter,
			final String pattern,
			final String options) {
		return com.mongodb.client.model.Filters.regex(MethodReferenceResolver.resolveFieldName(getter), pattern,
				options);
	}

	/**
	 * Creates a regex filter using a getter method reference with a compiled {@link Pattern}.
	 *
	 * <pre>{@code
	 * // Case-insensitive match using compiled Pattern
	 * Pattern pattern = Pattern.compile("^john", Pattern.CASE_INSENSITIVE);
	 * Bson filter = Filters.regex(User::getName, pattern);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param getter the getter method reference (must return String)
	 * @param pattern the compiled regex pattern
	 * @return the regex filter
	 */
	public static <T> Bson regex(final SerializableFunction<T, String> getter, final Pattern pattern) {
		return com.mongodb.client.model.Filters.regex(MethodReferenceResolver.resolveFieldName(getter), pattern);
	}

	// ====================================================================
	// Array operators — Getter method reference overloads
	// ====================================================================

	/**
	 * Creates an all filter using a getter method reference for an array/collection field.
	 * Matches documents where the array contains all the specified values.
	 *
	 * <pre>{@code
	 * // Match documents where "tags" contains both "java" and "mongodb"
	 * List<String> required = List.of("java", "mongodb");
	 * Bson filter = Filters.all(Article::getTags, required);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the array element type
	 * @param getter the getter method reference (must return Iterable)
	 * @param values the iterable of values that must all be present
	 * @return the all filter
	 */
	public static <T, R> Bson all(
			final SerializableFunction<T, ? extends Iterable<R>> getter,
			final Iterable<R> values) {
		return com.mongodb.client.model.Filters.all(MethodReferenceResolver.resolveFieldName(getter), values);
	}

	/**
	 * Creates a size filter using a getter method reference for an array/collection field.
	 * Matches documents where the array has the specified number of elements.
	 *
	 * <pre>{@code
	 * // Match documents where "tags" array has exactly 3 elements
	 * Bson filter = Filters.size(Article::getTags, 3);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the array element type
	 * @param getter the getter method reference (must return Iterable)
	 * @param size the expected array size
	 * @return the size filter
	 */
	public static <T, R> Bson size(final SerializableFunction<T, ? extends Iterable<R>> getter, final int size) {
		return com.mongodb.client.model.Filters.size(MethodReferenceResolver.resolveFieldName(getter), size);
	}

	/**
	 * Creates an elemMatch filter using a getter method reference for an array/collection field.
	 * Matches documents where the array contains at least one element matching the sub-filter.
	 *
	 * <pre>{@code
	 * // Match users who have at least one address in "Paris"
	 * Bson filter = Filters.elemMatch(
	 *     User::getAddresses,
	 *     Filters.eq("city", "Paris")
	 * );
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the array element type
	 * @param getter the getter method reference (must return Iterable)
	 * @param filter the sub-filter to match array elements against
	 * @return the elemMatch filter
	 */
	public static <T, R> Bson elemMatch(
			final SerializableFunction<T, ? extends Iterable<R>> getter,
			final Bson filter) {
		return com.mongodb.client.model.Filters.elemMatch(MethodReferenceResolver.resolveFieldName(getter), filter);
	}

	// ====================================================================
	// Comparison operators — Setter method reference overloads
	// ====================================================================

	/**
	 * Creates an equality filter using a setter method reference.
	 * The field name is resolved from the method reference.
	 *
	 * <pre>{@code
	 * // Match documents where the "name" field equals "John"
	 * Bson filter = Filters.eq(User::setName, "John");
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the setter method reference
	 * @param value the value to match
	 * @return the equality filter
	 */
	public static <T, V> Bson eq(final SerializableBiConsumer<T, V> setter, final V value) {
		return com.mongodb.client.model.Filters.eq(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	/**
	 * Creates a not-equal filter using a setter method reference.
	 *
	 * <pre>{@code
	 * // Match documents where "status" is not "deleted"
	 * Bson filter = Filters.ne(User::setStatus, "deleted");
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the setter method reference
	 * @param value the value to exclude
	 * @return the not-equal filter
	 */
	public static <T, V> Bson ne(final SerializableBiConsumer<T, V> setter, final V value) {
		return com.mongodb.client.model.Filters.ne(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	/**
	 * Creates a greater-than filter using a setter method reference.
	 *
	 * <pre>{@code
	 * // Match documents where "age" is greater than 18
	 * Bson filter = Filters.gt(User::setAge, 18);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the setter method reference
	 * @param value the lower bound (exclusive)
	 * @return the greater-than filter
	 */
	public static <T, V> Bson gt(final SerializableBiConsumer<T, V> setter, final V value) {
		return com.mongodb.client.model.Filters.gt(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	/**
	 * Creates a greater-than-or-equal filter using a setter method reference.
	 *
	 * <pre>{@code
	 * // Match documents where "score" is at least 50
	 * Bson filter = Filters.gte(User::setScore, 50);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the setter method reference
	 * @param value the lower bound (inclusive)
	 * @return the greater-than-or-equal filter
	 */
	public static <T, V> Bson gte(final SerializableBiConsumer<T, V> setter, final V value) {
		return com.mongodb.client.model.Filters.gte(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	/**
	 * Creates a less-than filter using a setter method reference.
	 *
	 * <pre>{@code
	 * // Match documents where "price" is less than 100
	 * Bson filter = Filters.lt(Product::setPrice, 100.0);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the setter method reference
	 * @param value the upper bound (exclusive)
	 * @return the less-than filter
	 */
	public static <T, V> Bson lt(final SerializableBiConsumer<T, V> setter, final V value) {
		return com.mongodb.client.model.Filters.lt(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	/**
	 * Creates a less-than-or-equal filter using a setter method reference.
	 *
	 * <pre>{@code
	 * // Match documents where "quantity" is at most 10
	 * Bson filter = Filters.lte(Product::setQuantity, 10);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the setter method reference
	 * @param value the upper bound (inclusive)
	 * @return the less-than-or-equal filter
	 */
	public static <T, V> Bson lte(final SerializableBiConsumer<T, V> setter, final V value) {
		return com.mongodb.client.model.Filters.lte(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	// ====================================================================
	// Element operators — Setter method reference overloads
	// ====================================================================

	/**
	 * Creates a filter that matches documents containing the field resolved from
	 * the setter method reference.
	 *
	 * <pre>{@code
	 * // Match documents that have an "email" field
	 * Bson filter = Filters.exists(User::setEmail);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the setter method reference
	 * @return the exists filter
	 */
	public static <T, V> Bson exists(final SerializableBiConsumer<T, V> setter) {
		return com.mongodb.client.model.Filters.exists(MethodReferenceResolver.resolveFieldName(setter));
	}

	/**
	 * Creates a filter that matches documents based on whether the field resolved
	 * from the setter method reference exists or not.
	 *
	 * <pre>{@code
	 * // Match documents that have a "phone" field
	 * Bson filter = Filters.exists(User::setPhone, true);
	 *
	 * // Match documents that do NOT have a "deletedAt" field
	 * Bson filter = Filters.exists(User::setDeletedAt, false);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the setter method reference
	 * @param exists true to match documents with the field, false without
	 * @return the exists filter
	 */
	public static <T, V> Bson exists(final SerializableBiConsumer<T, V> setter, final boolean exists) {
		return com.mongodb.client.model.Filters.exists(MethodReferenceResolver.resolveFieldName(setter), exists);
	}

	// ====================================================================
	// Comparison operators — Fluent setter method reference overloads
	// ====================================================================

	/**
	 * Creates an equality filter using a fluent setter method reference.
	 *
	 * <pre>{@code
	 * // Match documents where "name" equals "John" (fluent setter)
	 * Bson filter = Filters.eq(User::setName, "John");
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the fluent setter method reference (returns entity for chaining)
	 * @param value the value to match
	 * @return the equality filter
	 */
	public static <T, V> Bson eq(final SerializableBiFunction<T, V, ?> setter, final V value) {
		return com.mongodb.client.model.Filters.eq(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	/**
	 * Creates a not-equal filter using a fluent setter method reference.
	 *
	 * <pre>{@code
	 * Bson filter = Filters.ne(User::setStatus, "deleted");
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the fluent setter method reference
	 * @param value the value to exclude
	 * @return the not-equal filter
	 */
	public static <T, V> Bson ne(final SerializableBiFunction<T, V, ?> setter, final V value) {
		return com.mongodb.client.model.Filters.ne(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	/**
	 * Creates a greater-than filter using a fluent setter method reference.
	 *
	 * <pre>{@code
	 * Bson filter = Filters.gt(User::setAge, 18);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the fluent setter method reference
	 * @param value the lower bound (exclusive)
	 * @return the greater-than filter
	 */
	public static <T, V> Bson gt(final SerializableBiFunction<T, V, ?> setter, final V value) {
		return com.mongodb.client.model.Filters.gt(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	/**
	 * Creates a greater-than-or-equal filter using a fluent setter method reference.
	 *
	 * <pre>{@code
	 * Bson filter = Filters.gte(User::setScore, 50);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the fluent setter method reference
	 * @param value the lower bound (inclusive)
	 * @return the greater-than-or-equal filter
	 */
	public static <T, V> Bson gte(final SerializableBiFunction<T, V, ?> setter, final V value) {
		return com.mongodb.client.model.Filters.gte(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	/**
	 * Creates a less-than filter using a fluent setter method reference.
	 *
	 * <pre>{@code
	 * Bson filter = Filters.lt(Product::setPrice, 100.0);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the fluent setter method reference
	 * @param value the upper bound (exclusive)
	 * @return the less-than filter
	 */
	public static <T, V> Bson lt(final SerializableBiFunction<T, V, ?> setter, final V value) {
		return com.mongodb.client.model.Filters.lt(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	/**
	 * Creates a less-than-or-equal filter using a fluent setter method reference.
	 *
	 * <pre>{@code
	 * Bson filter = Filters.lte(Product::setQuantity, 10);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the fluent setter method reference
	 * @param value the upper bound (inclusive)
	 * @return the less-than-or-equal filter
	 */
	public static <T, V> Bson lte(final SerializableBiFunction<T, V, ?> setter, final V value) {
		return com.mongodb.client.model.Filters.lte(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	// ====================================================================
	// Element operators — Fluent setter method reference overloads
	// ====================================================================

	/**
	 * Creates an exists filter using a fluent setter method reference.
	 *
	 * <pre>{@code
	 * Bson filter = Filters.exists(User::setEmail);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the fluent setter method reference
	 * @return the exists filter
	 */
	public static <T, V> Bson exists(final SerializableBiFunction<T, V, ?> setter) {
		return com.mongodb.client.model.Filters.exists(MethodReferenceResolver.resolveFieldName(setter));
	}

	/**
	 * Creates an exists filter using a fluent setter method reference.
	 *
	 * <pre>{@code
	 * Bson filter = Filters.exists(User::setPhone, true);
	 * Bson filter = Filters.exists(User::setDeletedAt, false);
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the field value type
	 * @param setter the fluent setter method reference
	 * @param exists true to match documents with the field, false without
	 * @return the exists filter
	 */
	public static <T, V> Bson exists(final SerializableBiFunction<T, V, ?> setter, final boolean exists) {
		return com.mongodb.client.model.Filters.exists(MethodReferenceResolver.resolveFieldName(setter), exists);
	}
}
