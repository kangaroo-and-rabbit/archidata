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
 * <h3>Method reference usage (type-safe, resolves DB field names):</h3>
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

	public static <TItem> Bson eq(final TItem value) {
		return com.mongodb.client.model.Filters.eq(value);
	}

	public static <TItem> Bson eq(final String fieldName, final TItem value) {
		return com.mongodb.client.model.Filters.eq(fieldName, value);
	}

	public static <TItem> Bson ne(final String fieldName, final TItem value) {
		return com.mongodb.client.model.Filters.ne(fieldName, value);
	}

	public static <TItem> Bson gt(final String fieldName, final TItem value) {
		return com.mongodb.client.model.Filters.gt(fieldName, value);
	}

	public static <TItem> Bson lt(final String fieldName, final TItem value) {
		return com.mongodb.client.model.Filters.lt(fieldName, value);
	}

	public static <TItem> Bson gte(final String fieldName, final TItem value) {
		return com.mongodb.client.model.Filters.gte(fieldName, value);
	}

	public static <TItem> Bson lte(final String fieldName, final TItem value) {
		return com.mongodb.client.model.Filters.lte(fieldName, value);
	}

	@SafeVarargs
	public static <TItem> Bson in(final String fieldName, final TItem... values) {
		return com.mongodb.client.model.Filters.in(fieldName, values);
	}

	public static <TItem> Bson in(final String fieldName, final Iterable<TItem> values) {
		return com.mongodb.client.model.Filters.in(fieldName, values);
	}

	@SafeVarargs
	public static <TItem> Bson nin(final String fieldName, final TItem... values) {
		return com.mongodb.client.model.Filters.nin(fieldName, values);
	}

	public static <TItem> Bson nin(final String fieldName, final Iterable<TItem> values) {
		return com.mongodb.client.model.Filters.nin(fieldName, values);
	}

	// ====================================================================
	// Logical operators
	// ====================================================================

	public static Bson and(final Iterable<Bson> filters) {
		return com.mongodb.client.model.Filters.and(filters);
	}

	public static Bson and(final Bson... filters) {
		return com.mongodb.client.model.Filters.and(filters);
	}

	public static Bson or(final Iterable<Bson> filters) {
		return com.mongodb.client.model.Filters.or(filters);
	}

	public static Bson or(final Bson... filters) {
		return com.mongodb.client.model.Filters.or(filters);
	}

	public static Bson not(final Bson filter) {
		return com.mongodb.client.model.Filters.not(filter);
	}

	public static Bson nor(final Bson... filters) {
		return com.mongodb.client.model.Filters.nor(filters);
	}

	public static Bson nor(final Iterable<Bson> filters) {
		return com.mongodb.client.model.Filters.nor(filters);
	}

	// ====================================================================
	// Element operators
	// ====================================================================

	public static Bson exists(final String fieldName) {
		return com.mongodb.client.model.Filters.exists(fieldName);
	}

	public static Bson exists(final String fieldName, final boolean exists) {
		return com.mongodb.client.model.Filters.exists(fieldName, exists);
	}

	public static Bson type(final String fieldName, final BsonType type) {
		return com.mongodb.client.model.Filters.type(fieldName, type);
	}

	public static Bson type(final String fieldName, final String type) {
		return com.mongodb.client.model.Filters.type(fieldName, type);
	}

	// ====================================================================
	// Evaluation operators
	// ====================================================================

	public static Bson mod(final String fieldName, final long divisor, final long remainder) {
		return com.mongodb.client.model.Filters.mod(fieldName, divisor, remainder);
	}

	public static Bson regex(final String fieldName, final String pattern) {
		return com.mongodb.client.model.Filters.regex(fieldName, pattern);
	}

	public static Bson regex(final String fieldName, final String pattern, final String options) {
		return com.mongodb.client.model.Filters.regex(fieldName, pattern, options);
	}

	public static Bson regex(final String fieldName, final Pattern pattern) {
		return com.mongodb.client.model.Filters.regex(fieldName, pattern);
	}

	public static Bson text(final String search) {
		return com.mongodb.client.model.Filters.text(search);
	}

	public static Bson where(final String javaScriptExpression) {
		return com.mongodb.client.model.Filters.where(javaScriptExpression);
	}

	public static <TExpression> Bson expr(final TExpression expression) {
		return com.mongodb.client.model.Filters.expr(expression);
	}

	// ====================================================================
	// Array operators
	// ====================================================================

	@SafeVarargs
	public static <TItem> Bson all(final String fieldName, final TItem... values) {
		return com.mongodb.client.model.Filters.all(fieldName, values);
	}

	public static <TItem> Bson all(final String fieldName, final Iterable<TItem> values) {
		return com.mongodb.client.model.Filters.all(fieldName, values);
	}

	public static Bson elemMatch(final String fieldName, final Bson filter) {
		return com.mongodb.client.model.Filters.elemMatch(fieldName, filter);
	}

	public static Bson size(final String fieldName, final int size) {
		return com.mongodb.client.model.Filters.size(fieldName, size);
	}

	// ====================================================================
	// Miscellaneous
	// ====================================================================

	public static Bson jsonSchema(final Bson schema) {
		return com.mongodb.client.model.Filters.jsonSchema(schema);
	}

	public static Bson empty() {
		return com.mongodb.client.model.Filters.empty();
	}

	// ====================================================================
	// Comparison operators — Getter method reference overloads
	// ====================================================================

	public static <T, R> Bson eq(final SerializableFunction<T, R> getter, final R value) {
		return com.mongodb.client.model.Filters.eq(MethodReferenceResolver.resolveFieldName(getter), value);
	}

	public static <T, R> Bson ne(final SerializableFunction<T, R> getter, final R value) {
		return com.mongodb.client.model.Filters.ne(MethodReferenceResolver.resolveFieldName(getter), value);
	}

	public static <T, R> Bson gt(final SerializableFunction<T, R> getter, final R value) {
		return com.mongodb.client.model.Filters.gt(MethodReferenceResolver.resolveFieldName(getter), value);
	}

	public static <T, R> Bson gte(final SerializableFunction<T, R> getter, final R value) {
		return com.mongodb.client.model.Filters.gte(MethodReferenceResolver.resolveFieldName(getter), value);
	}

	public static <T, R> Bson lt(final SerializableFunction<T, R> getter, final R value) {
		return com.mongodb.client.model.Filters.lt(MethodReferenceResolver.resolveFieldName(getter), value);
	}

	public static <T, R> Bson lte(final SerializableFunction<T, R> getter, final R value) {
		return com.mongodb.client.model.Filters.lte(MethodReferenceResolver.resolveFieldName(getter), value);
	}

	@SafeVarargs
	public static <T, R> Bson in(final SerializableFunction<T, R> getter, final R... values) {
		return com.mongodb.client.model.Filters.in(MethodReferenceResolver.resolveFieldName(getter), values);
	}

	public static <T, R> Bson in(final SerializableFunction<T, R> getter, final Iterable<R> values) {
		return com.mongodb.client.model.Filters.in(MethodReferenceResolver.resolveFieldName(getter), values);
	}

	@SafeVarargs
	public static <T, R> Bson nin(final SerializableFunction<T, R> getter, final R... values) {
		return com.mongodb.client.model.Filters.nin(MethodReferenceResolver.resolveFieldName(getter), values);
	}

	public static <T, R> Bson nin(final SerializableFunction<T, R> getter, final Iterable<R> values) {
		return com.mongodb.client.model.Filters.nin(MethodReferenceResolver.resolveFieldName(getter), values);
	}

	// ====================================================================
	// Element operators — Getter method reference overloads
	// ====================================================================

	public static <T, R> Bson exists(final SerializableFunction<T, R> getter) {
		return com.mongodb.client.model.Filters.exists(MethodReferenceResolver.resolveFieldName(getter));
	}

	public static <T, R> Bson exists(final SerializableFunction<T, R> getter, final boolean exists) {
		return com.mongodb.client.model.Filters.exists(MethodReferenceResolver.resolveFieldName(getter), exists);
	}

	// ====================================================================
	// Evaluation operators — Getter method reference overloads
	// ====================================================================

	public static <T> Bson regex(final SerializableFunction<T, String> getter, final String pattern) {
		return com.mongodb.client.model.Filters.regex(MethodReferenceResolver.resolveFieldName(getter), pattern);
	}

	public static <T> Bson regex(
			final SerializableFunction<T, String> getter,
			final String pattern,
			final String options) {
		return com.mongodb.client.model.Filters.regex(MethodReferenceResolver.resolveFieldName(getter), pattern,
				options);
	}

	public static <T> Bson regex(final SerializableFunction<T, String> getter, final Pattern pattern) {
		return com.mongodb.client.model.Filters.regex(MethodReferenceResolver.resolveFieldName(getter), pattern);
	}

	// ====================================================================
	// Array operators — Getter method reference overloads
	// ====================================================================

	public static <T, R> Bson all(
			final SerializableFunction<T, ? extends Iterable<R>> getter,
			final Iterable<R> values) {
		return com.mongodb.client.model.Filters.all(MethodReferenceResolver.resolveFieldName(getter), values);
	}

	public static <T, R> Bson size(final SerializableFunction<T, ? extends Iterable<R>> getter, final int size) {
		return com.mongodb.client.model.Filters.size(MethodReferenceResolver.resolveFieldName(getter), size);
	}

	public static <T, R> Bson elemMatch(
			final SerializableFunction<T, ? extends Iterable<R>> getter,
			final Bson filter) {
		return com.mongodb.client.model.Filters.elemMatch(MethodReferenceResolver.resolveFieldName(getter), filter);
	}

	// ====================================================================
	// Comparison operators — Setter method reference overloads
	// ====================================================================

	public static <T, V> Bson eq(final SerializableBiConsumer<T, V> setter, final V value) {
		return com.mongodb.client.model.Filters.eq(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	public static <T, V> Bson ne(final SerializableBiConsumer<T, V> setter, final V value) {
		return com.mongodb.client.model.Filters.ne(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	public static <T, V> Bson gt(final SerializableBiConsumer<T, V> setter, final V value) {
		return com.mongodb.client.model.Filters.gt(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	public static <T, V> Bson gte(final SerializableBiConsumer<T, V> setter, final V value) {
		return com.mongodb.client.model.Filters.gte(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	public static <T, V> Bson lt(final SerializableBiConsumer<T, V> setter, final V value) {
		return com.mongodb.client.model.Filters.lt(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	public static <T, V> Bson lte(final SerializableBiConsumer<T, V> setter, final V value) {
		return com.mongodb.client.model.Filters.lte(MethodReferenceResolver.resolveFieldName(setter), value);
	}

	// ====================================================================
	// Element operators — Setter method reference overloads
	// ====================================================================

	public static <T, V> Bson exists(final SerializableBiConsumer<T, V> setter) {
		return com.mongodb.client.model.Filters.exists(MethodReferenceResolver.resolveFieldName(setter));
	}

	public static <T, V> Bson exists(final SerializableBiConsumer<T, V> setter, final boolean exists) {
		return com.mongodb.client.model.Filters.exists(MethodReferenceResolver.resolveFieldName(setter), exists);
	}
}
