package org.atriasoft.archidata.bean.accessor;

/**
 * Typed functional interface for reading a property value from an object instance.
 *
 * <p>Unlike {@link PropertyGetter} which erases types to {@code Object}, this interface
 * preserves the bean type and property type for type-safe access without boxing for
 * reference types.
 *
 * @param <T> the bean (instance) type
 * @param <V> the property value type
 */
@FunctionalInterface
public interface TypedPropertyGetter<T, V> {

	/**
	 * Read the property value from the given instance.
	 *
	 * @param instance the object to read from (never null)
	 * @return the property value (may be null for reference types)
	 * @throws Throwable if the underlying accessor fails
	 */
	V get(T instance) throws Throwable;
}
