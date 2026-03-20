package org.atriasoft.archidata.dataAccess.options;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.dataAccess.MethodReferenceResolver;
import org.atriasoft.archidata.dataAccess.SerializableBiConsumer;
import org.atriasoft.archidata.dataAccess.FieldRef;
import org.atriasoft.archidata.dataAccess.SerializableBiFunction;
import org.atriasoft.archidata.dataAccess.SerializableFunction;

/**
 * Whitelist option specifying which fields to include in update operations.
 *
 * <p>Supports both string-based and type-safe method reference constructors:
 * <pre>{@code
 * // String-based (existing)
 * new FilterValue("name", "email")
 *
 * // Type-safe getter references (resolves @Column annotations)
 * new FilterValue(User::getName, User::getEmail)
 *
 * // Type-safe setter references
 * new FilterValue(User::setName, User::setEmail)
 * }</pre>
 *
 * @see org.atriasoft.archidata.dataAccess.Fields
 */
public class FilterValue extends QueryOption {
	public final List<String> filterValue;

	public FilterValue(final List<String> filterValue) {
		this.filterValue = filterValue;
	}

	public FilterValue(final String... filterValue) {
		this.filterValue = List.of(filterValue);
	}

	/**
	 * Create a FilterValue from getter method references.
	 *
	 * <pre>{@code
	 * new FilterValue(User::getName, User::getEmail)
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param getters getter method references
	 */
	@SafeVarargs
	public <T> FilterValue(final SerializableFunction<T, ?>... getters) {
		final List<String> resolved = new ArrayList<>(getters.length);
		for (final SerializableFunction<T, ?> getter : getters) {
			resolved.add(MethodReferenceResolver.resolveFieldName(getter));
		}
		this.filterValue = resolved;
	}

	/**
	 * Create a FilterValue from setter method references.
	 *
	 * <pre>{@code
	 * new FilterValue(User::setName, User::setEmail)
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param setters setter method references
	 */
	@SafeVarargs
	public <T> FilterValue(final SerializableBiConsumer<T, ?>... setters) {
		final List<String> resolved = new ArrayList<>(setters.length);
		for (final SerializableBiConsumer<T, ?> setter : setters) {
			resolved.add(MethodReferenceResolver.resolveFieldName(setter));
		}
		this.filterValue = resolved;
	}

	/**
	 * Create a FilterValue from fluent setter method references.
	 *
	 * <pre>{@code
	 * new FilterValue(User::setName, User::setEmail)  // fluent setters
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param setters fluent setter method references
	 */
	@SafeVarargs
	public <T> FilterValue(final SerializableBiFunction<T, ?, ?>... setters) {
		final List<String> resolved = new ArrayList<>(setters.length);
		for (final SerializableBiFunction<T, ?, ?> setter : setters) {
			resolved.add(MethodReferenceResolver.resolveFieldName(setter));
		}
		this.filterValue = resolved;
	}

	/**
	 * Create a FilterValue from mixed field references (getter, void setter, fluent setter).
	 *
	 * <pre>{@code
	 * new FilterValue(FieldRef.of(User::getName), FieldRef.of(User::setEmail))
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param refs field references
	 */
	@SafeVarargs
	public <T> FilterValue(final FieldRef<T>... refs) {
		final List<String> resolved = new ArrayList<>(refs.length);
		for (final FieldRef<T> ref : refs) {
			resolved.add(ref.getFieldName());
		}
		this.filterValue = resolved;
	}

	public List<String> getValues() {
		return this.filterValue;
	}

	public static FilterValue getEditableFieldsNames(final Class<?> clazz) {
		return new FilterValue(AnnotationTools.getFieldsNamesFilter(clazz, false));
	}

	public static FilterValue getAllFieldsNames(final Class<?> clazz) {
		return new FilterValue(AnnotationTools.getFieldsNamesFilter(clazz, true));
	}
}
