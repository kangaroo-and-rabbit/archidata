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
 * Whitelist option specifying which top-level fields to include.
 *
 * <p>On an update it selects the fields to write; on a read it becomes the MongoDB projection, so
 * an unlisted field is never transferred nor mapped (the primary key is always read). Field names
 * are the structural ones — the same on both sides — and an unknown one is rejected.
 *
 * <p>On a read, an entry may also be a dotted path addressing an embedded sub-document
 * ({@code "address.city"}): only that part of the sub-document is transferred. See
 * {@link org.atriasoft.archidata.dataAccess.model.ReadFieldSelector}.
 *
 * <p>Supports both string-based and type-safe method reference constructors:
 * <pre>{@code
 * // String-based (existing)
 * new FilterValue("name", "email")
 *
 * // Type-safe getter references (resolves @Field annotations)
 * new FilterValue(User::getName, User::getEmail)
 *
 * // Type-safe setter references
 * new FilterValue(User::setName, User::setEmail)
 * }</pre>
 *
 * @see org.atriasoft.archidata.dataAccess.Fields
 */
public class FilterValue extends QueryOption {
	/** The list of field names to include. */
	public final List<String> filterValue;
	/** Whether this list is the default one of a full update, see {@link #isFullUpdate()}. */
	private final boolean fullUpdate;

	private FilterValue(final List<String> filterValue, final boolean fullUpdate) {
		this.filterValue = filterValue;
		this.fullUpdate = fullUpdate;
	}

	/**
	 * Creates a FilterValue from a list of field names.
	 *
	 * @param filterValue the list of field names to include
	 */
	public FilterValue(final List<String> filterValue) {
		this(filterValue, false);
	}

	/**
	 * Creates a FilterValue from field name varargs.
	 *
	 * @param filterValue the field names to include
	 */
	public FilterValue(final String... filterValue) {
		this(List.of(filterValue), false);
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
		this.fullUpdate = false;
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
		this.fullUpdate = false;
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
		this.fullUpdate = false;
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
		this.fullUpdate = false;
	}

	/**
	 * Returns the list of field names to include.
	 *
	 * @return the list of included field names
	 */
	public List<String> getValues() {
		return this.filterValue;
	}

	/**
	 * Tells whether this is the field list of a full update ({@link #getEditableFieldsNames(Class)}, the
	 * default of {@code updateById} without {@code FilterValue}) rather than a list of names chosen by the caller.
	 *
	 * <p>A full update never erases what a standard read hides: an embedded object holding {@code @DataNotRead}
	 * or {@code @DataEncrypt} fields is written field by field in dot notation, its protected fields left as
	 * stored, and a list, set or map of such objects is not written. Name the field in a {@code FilterValue}
	 * to rewrite it as a whole.
	 *
	 * @return {@code true} for the field list of a full update
	 */
	public boolean isFullUpdate() {
		return this.fullUpdate;
	}

	/**
	 * Creates a FilterValue containing only the editable field names of the given class.
	 *
	 * <p>Excluded: the system-managed fields (primary key, timestamps, deleted marker) and the protected
	 * fields ({@code @DataNotRead}, {@code @DataEncrypt}). This is the default field list of
	 * {@code updateById} without {@code FilterValue}: a protected field is written only when a
	 * {@code FilterValue} names it. The list is marked as a full update ({@link #isFullUpdate()}): the
	 * protected fields of embedded objects are kept too.
	 *
	 * @param clazz the entity class to inspect
	 * @return a FilterValue with the editable field names
	 */
	public static FilterValue getEditableFieldsNames(final Class<?> clazz) {
		return new FilterValue(AnnotationTools.getFieldsNamesFilter(clazz, false), true);
	}

	/**
	 * Creates a FilterValue containing all field names of the given class.
	 *
	 * @param clazz the entity class to inspect
	 * @return a FilterValue with all field names
	 */
	public static FilterValue getAllFieldsNames(final Class<?> clazz) {
		return new FilterValue(AnnotationTools.getFieldsNamesFilter(clazz, true));
	}
}
