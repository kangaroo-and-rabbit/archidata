package org.atriasoft.archidata.dataAccess.options;

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
 * // Type-safe getter reference (resolves @Column annotations)
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
	 * @param value the field name to sort by
	 * @param order the sort direction
	 */
	public OrderItem(final String value, final Order order) {
		this.value = value;
		this.order = order;
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
