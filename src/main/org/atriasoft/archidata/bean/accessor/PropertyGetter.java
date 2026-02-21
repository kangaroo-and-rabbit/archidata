package org.atriasoft.archidata.bean.accessor;

/**
 * Functional interface for reading a property value from an object instance.
 *
 * <p>Implementations may be generated via {@code LambdaMetafactory} for near-native
 * performance, or may fall back to {@code MethodHandle} / reflection.
 */
@FunctionalInterface
public interface PropertyGetter {

	/**
	 * Read the property value from the given instance.
	 *
	 * @param instance the object to read from (never null)
	 * @return the property value (may be null for reference types)
	 * @throws Throwable if the underlying accessor fails
	 */
	Object get(Object instance) throws Throwable;
}
