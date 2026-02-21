package org.atriasoft.archidata.bean.accessor;

/**
 * Functional interface for writing a property value to an object instance.
 *
 * <p>Implementations may be generated via {@code LambdaMetafactory} for near-native
 * performance, or may fall back to {@code MethodHandle} / reflection.
 */
@FunctionalInterface
public interface PropertySetter {

	/**
	 * Write the property value to the given instance.
	 *
	 * @param instance the object to write to (never null)
	 * @param value    the value to set (may be null for reference types)
	 * @throws Throwable if the underlying accessor fails
	 */
	void set(Object instance, Object value) throws Throwable;
}
