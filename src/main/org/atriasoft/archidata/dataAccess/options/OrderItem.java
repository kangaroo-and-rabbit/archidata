package org.atriasoft.archidata.dataAccess.options;

import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.MethodReferenceResolver;
import org.atriasoft.archidata.dataAccess.SerializableBiConsumer;
import org.atriasoft.archidata.dataAccess.SerializableBiFunction;
import org.atriasoft.archidata.dataAccess.SerializableFunction;

/**
 * Represents a single sort criterion with a field name and direction.
 *
 * <p>Supports both string-based and type-safe method reference constructors:
 * <pre>{@code
 * // String-based
 * new OrderItem("name", Order.ASC)
 *
 * // Type-safe getter reference (resolves @Field annotations)
 * new OrderItem(User::getName, Order.ASC)
 *
 * // Type-safe setter reference
 * new OrderItem(User::setName, Order.DESC)
 * }</pre>
 *
 * @see OrderBy
 */
public class OrderItem {
	/** Sort direction for query results. */
	public enum Order {
		/** Ascending sort order. */
		ASC,
		/** Descending sort order. */
		DESC
	}

	/** The field name to sort by. */
	public final String value;
	/** The sort direction. */
	public final Order order;

	/**
	 * Constructs an OrderItem with the specified field name and sort direction.
	 *
	 * <p>The name is checked by {@link Filters#checkFieldName(String)} (no leading {@code $}, no NUL). For a
	 * name chosen by a client, use {@link #ofClientField(Class, String, Order)}.
	 *
	 * @param value the field name to sort by
	 * @param order the sort direction
	 * @throws IllegalArgumentException if the name is invalid
	 */
	public OrderItem(final String value, final Order order) {
		this.value = Filters.checkFieldName(value);
		this.order = order;
	}

	/**
	 * Creates an OrderItem on a field name sent by a client, checked against the model of the entity.
	 *
	 * <p>The field must exist and must not be protected ({@code @DataNotRead}, {@code @DataEncrypt}): sorting
	 * on a hidden field reveals the order of its values. See {@link Filters#checkFieldName(Class, String)}.
	 *
	 * @param clazz the entity class
	 * @param fieldName the database field name sent by the client
	 * @param order the sort direction
	 * @return the sort criterion
	 * @throws IllegalArgumentException if the name is invalid, unknown or protected
	 */
	public static OrderItem ofClientField(final Class<?> clazz, final String fieldName, final Order order) {
		return new OrderItem(Filters.checkFieldName(clazz, fieldName), order);
	}

	/**
	 * Create an OrderItem from a getter method reference.
	 *
	 * <pre>{@code
	 * new OrderItem(User::getName, Order.ASC)
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the property type
	 * @param getter a serializable getter reference
	 * @param order the sort direction
	 */
	public <T, R> OrderItem(final SerializableFunction<T, R> getter, final Order order) {
		this.value = MethodReferenceResolver.resolveFieldName(getter);
		this.order = order;
	}

	/**
	 * Create an OrderItem from a setter method reference.
	 *
	 * <pre>{@code
	 * new OrderItem(User::setName, Order.DESC)
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the property type
	 * @param setter a serializable setter reference
	 * @param order the sort direction
	 */
	public <T, V> OrderItem(final SerializableBiConsumer<T, V> setter, final Order order) {
		this.value = MethodReferenceResolver.resolveFieldName(setter);
		this.order = order;
	}

	/**
	 * Create an OrderItem from a fluent setter method reference.
	 *
	 * <pre>{@code
	 * new OrderItem(User::setName, Order.DESC)  // fluent setter
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the property type
	 * @param setter a serializable fluent setter reference
	 * @param order the sort direction
	 */
	public <T, V> OrderItem(final SerializableBiFunction<T, V, ?> setter, final Order order) {
		this.value = MethodReferenceResolver.resolveFieldName(setter);
		this.order = order;
	}
}
