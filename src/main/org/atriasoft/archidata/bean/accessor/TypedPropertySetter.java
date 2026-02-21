package org.atriasoft.archidata.bean.accessor;

/**
 * Typed functional interface for writing a property value to an object instance.
 *
 * <p>Unlike {@link PropertySetter} which erases types to {@code Object}, this interface
 * preserves the bean type and property type for type-safe access without boxing for
 * reference types.
 *
 * @param <T> the bean (instance) type
 * @param <V> the property value type
 */
@FunctionalInterface
public interface TypedPropertySetter<T, V> {

	/**
	 * Write the property value to the given instance.
	 *
	 * @param instance the object to write to (never null)
	 * @param value    the value to set (may be null for reference types)
	 * @throws Throwable if the underlying accessor fails
	 */
	void set(T instance, V value) throws Throwable;
}
