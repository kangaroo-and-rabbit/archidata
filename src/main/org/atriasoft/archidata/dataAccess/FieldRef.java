package org.atriasoft.archidata.dataAccess;

/**
 * A resolved field reference that wraps a database field name obtained from a getter,
 * void setter, or fluent setter method reference.
 *
 * <p>This wrapper enables mixing different method reference types in a single varargs call:
 * <pre>{@code
 * new FilterValue(FieldRef.of(User::getName), FieldRef.of(User::setEmail), FieldRef.of(User::setAge))
 * }</pre>
 *
 * @param <T> the entity type
 * @see Fields
 * @see MethodReferenceResolver
 */
public final class FieldRef<T> {
	private final String fieldName;

	private FieldRef(final String fieldName) {
		this.fieldName = fieldName;
	}

	/**
	 * Create a FieldRef from a getter method reference.
	 *
	 * @param <T> the entity type
	 * @param <R> the return type
	 * @param getter a serializable getter reference (e.g. User::getName)
	 * @return a FieldRef wrapping the resolved database field name
	 */
	public static <T, R> FieldRef<T> of(final SerializableFunction<T, R> getter) {
		return new FieldRef<>(MethodReferenceResolver.resolveFieldName(getter));
	}

	/**
	 * Create a FieldRef from a void setter method reference.
	 *
	 * @param <T> the entity type
	 * @param <V> the value type
	 * @param setter a serializable setter reference (e.g. User::setName)
	 * @return a FieldRef wrapping the resolved database field name
	 */
	public static <T, V> FieldRef<T> of(final SerializableBiConsumer<T, V> setter) {
		return new FieldRef<>(MethodReferenceResolver.resolveFieldName(setter));
	}

	/**
	 * Create a FieldRef from a fluent setter method reference.
	 *
	 * @param <T> the entity type
	 * @param <V> the value type
	 * @param <R> the return type (typically the entity class)
	 * @param setter a serializable fluent setter reference (e.g. User::setName where setName returns User)
	 * @return a FieldRef wrapping the resolved database field name
	 */
	public static <T, V, R> FieldRef<T> of(final SerializableBiFunction<T, V, R> setter) {
		return new FieldRef<>(MethodReferenceResolver.resolveFieldName(setter));
	}

	/**
	 * @return the resolved database field name
	 */
	public String getFieldName() {
		return this.fieldName;
	}
}
