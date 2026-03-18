package org.atriasoft.archidata.dataAccess.options;

import java.util.List;

import org.atriasoft.archidata.dataAccess.MethodReferenceResolver;
import org.atriasoft.archidata.dataAccess.SerializableBiConsumer;
import org.atriasoft.archidata.dataAccess.SerializableFunction;
import org.atriasoft.archidata.dataAccess.options.OrderItem.Order;
import org.bson.Document;

/**
 * Sort option for database queries.
 *
 * <p>Supports both traditional construction and fluent factory methods:
 * <pre>{@code
 * // Traditional
 * new OrderBy(new OrderItem("name", Order.ASC))
 *
 * // Type-safe with OrderItem
 * new OrderBy(new OrderItem(User::getName, Order.ASC))
 *
 * // Fluent factory methods
 * OrderBy.asc(User::getName)
 * OrderBy.desc(User::getAge)
 * }</pre>
 *
 * @see OrderItem
 */
public class OrderBy extends QueryOption {
	protected final List<OrderItem> childs;

	public OrderBy(final List<OrderItem> childs) {
		this.childs = childs;
	}

	public OrderBy(final OrderItem... childs) {
		this.childs = List.of(childs);
	}

	/**
	 * Create an ascending OrderBy from a getter method reference.
	 *
	 * <pre>{@code
	 * da.gets(User.class, OrderBy.asc(User::getName))
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the property type
	 * @param getter a serializable getter reference
	 * @return an OrderBy with ascending sort on the resolved field
	 */
	public static <T, R> OrderBy asc(final SerializableFunction<T, R> getter) {
		return new OrderBy(new OrderItem(MethodReferenceResolver.resolveFieldName(getter), Order.ASC));
	}

	/**
	 * Create a descending OrderBy from a getter method reference.
	 *
	 * <pre>{@code
	 * da.gets(User.class, OrderBy.desc(User::getAge))
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the property type
	 * @param getter a serializable getter reference
	 * @return an OrderBy with descending sort on the resolved field
	 */
	public static <T, R> OrderBy desc(final SerializableFunction<T, R> getter) {
		return new OrderBy(new OrderItem(MethodReferenceResolver.resolveFieldName(getter), Order.DESC));
	}

	/**
	 * Create an ascending OrderBy from a setter method reference.
	 *
	 * @param <T> the entity type
	 * @param <V> the property type
	 * @param setter a serializable setter reference
	 * @return an OrderBy with ascending sort on the resolved field
	 */
	public static <T, V> OrderBy asc(final SerializableBiConsumer<T, V> setter) {
		return new OrderBy(new OrderItem(MethodReferenceResolver.resolveFieldName(setter), Order.ASC));
	}

	/**
	 * Create a descending OrderBy from a setter method reference.
	 *
	 * @param <T> the entity type
	 * @param <V> the property type
	 * @param setter a serializable setter reference
	 * @return an OrderBy with descending sort on the resolved field
	 */
	public static <T, V> OrderBy desc(final SerializableBiConsumer<T, V> setter) {
		return new OrderBy(new OrderItem(MethodReferenceResolver.resolveFieldName(setter), Order.DESC));
	}

	public void generateSort(final Document data) {
		for (final OrderItem elem : this.childs) {
			data.append(elem.value, elem.order == Order.ASC ? 1 : -1);
		}
	}
}
