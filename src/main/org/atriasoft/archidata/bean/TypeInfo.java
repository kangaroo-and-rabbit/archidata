package org.atriasoft.archidata.bean;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable descriptor for a resolved Java type, including generic type parameters.
 *
 * <p>For a field declared as {@code List<String>}, this would yield:
 * {@code rawType=List.class, elementType=String.class, keyType=null}.
 *
 * <p>For {@code Map<String, Integer>}: {@code rawType=Map.class, elementType=Integer.class, keyType=String.class}.
 */
public record TypeInfo(
		/** The raw (erased) type. */
		Class<?> rawType,
		/** For List/Set/Collection/Optional → the element type. For Map → the value type. For arrays → the component type. Otherwise null. */
		Class<?> elementType,
		/** For Map → the key type. Otherwise null. */
		Class<?> keyType,
		/** The full generic Type for advanced use cases. */
		Type genericType) {

	public boolean isList() {
		return List.class.isAssignableFrom(this.rawType);
	}

	public boolean isSet() {
		return Set.class.isAssignableFrom(this.rawType);
	}

	public boolean isCollection() {
		return Collection.class.isAssignableFrom(this.rawType);
	}

	public boolean isMap() {
		return Map.class.isAssignableFrom(this.rawType);
	}

	public boolean isArray() {
		return this.rawType.isArray();
	}

	public boolean isEnum() {
		return this.rawType.isEnum();
	}

	public boolean isRecord() {
		return Record.class.isAssignableFrom(this.rawType);
	}

	public boolean isPrimitive() {
		return this.rawType.isPrimitive();
	}

	public boolean isOptional() {
		return Optional.class.isAssignableFrom(this.rawType);
	}

	// --- Factory methods ---

	/**
	 * Extract TypeInfo from a Field's declared type and generic type.
	 */
	public static TypeInfo fromField(final Field field) {
		return resolve(field.getType(), field.getGenericType());
	}

	/**
	 * Extract TypeInfo from a Method's return type.
	 */
	public static TypeInfo fromReturnType(final Method method) {
		return resolve(method.getReturnType(), method.getGenericReturnType());
	}

	/**
	 * Extract TypeInfo from a Method's first parameter type.
	 */
	public static TypeInfo fromFirstParameter(final Method method) {
		if (method.getParameterCount() == 0) {
			throw new IllegalArgumentException("Method has no parameters: " + method);
		}
		return resolve(method.getParameterTypes()[0], method.getGenericParameterTypes()[0]);
	}

	/**
	 * Extract TypeInfo from a Constructor parameter at the given index.
	 */
	public static TypeInfo fromConstructorParameter(final Constructor<?> constructor, final int paramIndex) {
		return resolve(constructor.getParameterTypes()[paramIndex], constructor.getGenericParameterTypes()[paramIndex]);
	}

	/**
	 * Create a TypeInfo from a generic {@link Type} (may be ParameterizedType or Class).
	 */
	public static TypeInfo fromType(final Type type) {
		if (type instanceof final Class<?> clazz) {
			return ofRaw(clazz);
		}
		if (type instanceof final ParameterizedType pt) {
			final Class<?> rawType = (Class<?>) pt.getRawType();
			return resolve(rawType, type);
		}
		// Fallback
		try {
			return ofRaw(Class.forName(type.getTypeName()));
		} catch (final ClassNotFoundException e) {
			throw new IllegalArgumentException("Cannot resolve Type: " + type, e);
		}
	}

	/**
	 * Create a simple TypeInfo for a raw class with no generics.
	 */
	public static TypeInfo ofRaw(final Class<?> clazz) {
		if (clazz.isArray()) {
			return new TypeInfo(clazz, clazz.getComponentType(), null, clazz);
		}
		return new TypeInfo(clazz, null, null, clazz);
	}

	// --- Internal ---

	private static TypeInfo resolve(final Class<?> rawType, final Type genericType) {
		// Array: component type as elementType
		if (rawType.isArray()) {
			return new TypeInfo(rawType, rawType.getComponentType(), null, genericType);
		}

		// Map<K,V>: keyType=K, elementType=V
		if (Map.class.isAssignableFrom(rawType) && genericType instanceof final ParameterizedType pt) {
			final Type[] args = pt.getActualTypeArguments();
			final Class<?> keyType = args.length > 0 ? resolveClass(args[0]) : null;
			final Class<?> valueType = args.length > 1 ? resolveClass(args[1]) : null;
			return new TypeInfo(rawType, valueType, keyType, genericType);
		}

		// Collection (List, Set, etc.) or Optional: elementType is first type arg
		if ((Collection.class.isAssignableFrom(rawType) || Optional.class.isAssignableFrom(rawType))
				&& genericType instanceof final ParameterizedType pt) {
			final Type[] args = pt.getActualTypeArguments();
			final Class<?> elementType = args.length > 0 ? resolveClass(args[0]) : null;
			return new TypeInfo(rawType, elementType, null, genericType);
		}

		// Any other ParameterizedType (e.g. custom generic): store first type arg as elementType
		if (genericType instanceof final ParameterizedType pt) {
			final Type[] args = pt.getActualTypeArguments();
			final Class<?> elementType = args.length > 0 ? resolveClass(args[0]) : null;
			return new TypeInfo(rawType, elementType, null, genericType);
		}

		// Simple type
		return new TypeInfo(rawType, null, null, genericType);
	}

	private static Class<?> resolveClass(final Type type) {
		if (type instanceof final Class<?> c) {
			return c;
		}
		if (type instanceof final ParameterizedType pt) {
			final Type raw = pt.getRawType();
			if (raw instanceof final Class<?> c) {
				return c;
			}
		}
		// Fallback: try by name
		try {
			return Class.forName(type.getTypeName());
		} catch (final ClassNotFoundException e) {
			return null;
		}
	}
}
