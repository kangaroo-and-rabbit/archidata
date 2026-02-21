package org.atriasoft.archidata.bean;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.atriasoft.archidata.bean.exception.IntrospectionException;

/**
 * Immutable descriptor for a constructor of a Java class.
 *
 * <p>Stores the constructor reference, parameter names, and type information
 * for intelligent object instantiation from partial data.
 */
public record ConstructorDescriptor(
		/** The actual constructor. */
		Constructor<?> constructor,
		/** Parameter names (extracted from bytecode or Record component names). */
		String[] parameterNames,
		/** Type information for each parameter. */
		TypeInfo[] parameterTypes) {

	/**
	 * The number of parameters.
	 */
	public int parameterCount() {
		return this.parameterNames.length;
	}

	/**
	 * Check if this constructor has no parameters.
	 */
	public boolean isNoArg() {
		return this.parameterNames.length == 0;
	}

	/**
	 * Create a new instance using this constructor with the given arguments.
	 *
	 * @param args the constructor arguments (must match parameter count and types)
	 * @return the new instance
	 * @throws IntrospectionException if instantiation fails
	 */
	public Object newInstance(final Object... args) throws IntrospectionException {
		try {
			return this.constructor.newInstance(args);
		} catch (final InstantiationException | IllegalAccessException | IllegalArgumentException e) {
			throw new IntrospectionException(
					"Failed to instantiate " + this.constructor.getDeclaringClass().getSimpleName()
							+ " with " + args.length + " args",
					e);
		} catch (final InvocationTargetException e) {
			throw new IntrospectionException(
					"Constructor of " + this.constructor.getDeclaringClass().getSimpleName() + " threw an exception",
					e.getCause());
		}
	}

	/**
	 * Find the index of a parameter by name.
	 *
	 * @return the index, or -1 if not found
	 */
	public int indexOfParameter(final String name) {
		for (int i = 0; i < this.parameterNames.length; i++) {
			if (this.parameterNames[i].equals(name)) {
				return i;
			}
		}
		return -1;
	}
}
