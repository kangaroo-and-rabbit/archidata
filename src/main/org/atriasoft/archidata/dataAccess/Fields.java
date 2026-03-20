package org.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.List;

/**
 * Type-safe utility for resolving entity method references to database field names.
 *
 * <p>Provides a convenient API to obtain database field names from getter or setter
 * method references, respecting {@code @Column(name = "...")} annotations.
 * All resolution is delegated to {@link MethodReferenceResolver}.
 *
 * <h2>Single field name:</h2>
 * <pre>{@code
 * String fieldName = Fields.of(User::getName);       // -> "name"
 * String fieldName = Fields.of(User::setName);       // -> "name"
 * String fieldName = Fields.of(User::getFullName);   // -> "full_name" (if @Column(name="full_name"))
 * }</pre>
 *
 * <h2>List of field names:</h2>
 * <pre>{@code
 * List<String> names = Fields.list(User::getName, User::getAge);  // -> ["name", "age"]
 * }</pre>
 *
 * @see MethodReferenceResolver
 * @see Filters
 */
public final class Fields {

	private Fields() {}

	/**
	 * Resolve a getter method reference to its database field name.
	 *
	 * <pre>{@code
	 * String name = Fields.of(User::getName);  // -> "name"
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <R> the property type
	 * @param getter a serializable getter reference
	 * @return the database field name
	 */
	public static <T, R> String of(final SerializableFunction<T, R> getter) {
		return MethodReferenceResolver.resolveFieldName(getter);
	}

	/**
	 * Resolve a setter method reference to its database field name.
	 *
	 * <pre>{@code
	 * String name = Fields.of(User::setName);  // -> "name"
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the property type
	 * @param setter a serializable setter reference
	 * @return the database field name
	 */
	public static <T, V> String of(final SerializableBiConsumer<T, V> setter) {
		return MethodReferenceResolver.resolveFieldName(setter);
	}

	/**
	 * Resolve multiple getter method references to a list of database field names.
	 *
	 * <pre>{@code
	 * List<String> names = Fields.list(User::getName, User::getAge);  // -> ["name", "age"]
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param getters serializable getter references
	 * @return list of database field names
	 */
	@SafeVarargs
	public static <T> List<String> list(final SerializableFunction<T, ?>... getters) {
		final List<String> result = new ArrayList<>(getters.length);
		for (final SerializableFunction<T, ?> getter : getters) {
			result.add(MethodReferenceResolver.resolveFieldName(getter));
		}
		return result;
	}

	/**
	 * Resolve multiple setter method references to a list of database field names.
	 *
	 * <pre>{@code
	 * List<String> names = Fields.list(User::setName, User::setAge);  // -> ["name", "age"]
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param setters serializable setter references
	 * @return list of database field names
	 */
	@SafeVarargs
	public static <T> List<String> list(final SerializableBiConsumer<T, ?>... setters) {
		final List<String> result = new ArrayList<>(setters.length);
		for (final SerializableBiConsumer<T, ?> setter : setters) {
			result.add(MethodReferenceResolver.resolveFieldName(setter));
		}
		return result;
	}

	/**
	 * Resolve a fluent setter method reference to its database field name.
	 *
	 * <pre>{@code
	 * String name = Fields.of(User::setName);  // -> "name" (fluent setter)
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param <V> the property type
	 * @param setter a serializable fluent setter reference
	 * @return the database field name
	 */
	public static <T, V> String of(final SerializableBiFunction<T, V, ?> setter) {
		return MethodReferenceResolver.resolveFieldName(setter);
	}

	/**
	 * Resolve multiple fluent setter method references to a list of database field names.
	 *
	 * @param <T> the entity type
	 * @param setters serializable fluent setter references
	 * @return list of database field names
	 */
	@SafeVarargs
	public static <T> List<String> list(final SerializableBiFunction<T, ?, ?>... setters) {
		final List<String> result = new ArrayList<>(setters.length);
		for (final SerializableBiFunction<T, ?, ?> setter : setters) {
			result.add(MethodReferenceResolver.resolveFieldName(setter));
		}
		return result;
	}

	/**
	 * Resolve a {@link FieldRef} to its database field name.
	 *
	 * @param <T> the entity type
	 * @param ref a field reference
	 * @return the database field name
	 */
	public static <T> String of(final FieldRef<T> ref) {
		return ref.getFieldName();
	}

	/**
	 * Resolve multiple {@link FieldRef} instances to a list of database field names.
	 * Allows mixing getter, void setter, and fluent setter references.
	 *
	 * <pre>{@code
	 * List<String> names = Fields.list(FieldRef.of(User::getName), FieldRef.of(User::setAge));
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param refs field references
	 * @return list of database field names
	 */
	@SafeVarargs
	public static <T> List<String> list(final FieldRef<T>... refs) {
		final List<String> result = new ArrayList<>(refs.length);
		for (final FieldRef<T> ref : refs) {
			result.add(ref.getFieldName());
		}
		return result;
	}
}
