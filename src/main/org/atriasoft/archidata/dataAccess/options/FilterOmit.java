package org.atriasoft.archidata.dataAccess.options;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.MethodReferenceResolver;
import org.atriasoft.archidata.dataAccess.SerializableBiConsumer;
import org.atriasoft.archidata.dataAccess.FieldRef;
import org.atriasoft.archidata.dataAccess.SerializableBiFunction;
import org.atriasoft.archidata.dataAccess.SerializableFunction;

/**
 * Blacklist option specifying which fields to exclude from update operations.
 *
 * <p>Supports both string-based and type-safe method reference constructors:
 * <pre>{@code
 * // String-based (existing)
 * new FilterOmit("password", "secret")
 *
 * // Type-safe getter references (resolves @Column annotations)
 * new FilterOmit(User::getPassword, User::getSecret)
 *
 * // Type-safe setter references
 * new FilterOmit(User::setPassword, User::setSecret)
 * }</pre>
 *
 * @see org.atriasoft.archidata.dataAccess.Fields
 */
public class FilterOmit extends QueryOption {
	/** The list of field names to exclude from update operations. */
	public final List<String> filterValue;

	/**
	 * Creates a FilterOmit from a list of field names.
	 *
	 * @param filterValue the list of field names to exclude
	 */
	public FilterOmit(final List<String> filterValue) {
		this.filterValue = filterValue;
	}

	/**
	 * Creates a FilterOmit from field name varargs.
	 *
	 * @param filterValue the field names to exclude
	 */
	public FilterOmit(final String... filterValue) {
		this.filterValue = List.of(filterValue);
	}

	/**
	 * Create a FilterOmit from getter method references.
	 *
	 * <pre>{@code
	 * new FilterOmit(User::getPassword, User::getSecret)
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param getters getter method references
	 */
	@SafeVarargs
	public <T> FilterOmit(final SerializableFunction<T, ?>... getters) {
		final List<String> resolved = new ArrayList<>(getters.length);
		for (final SerializableFunction<T, ?> getter : getters) {
			resolved.add(MethodReferenceResolver.resolveFieldName(getter));
		}
		this.filterValue = resolved;
	}

	/**
	 * Create a FilterOmit from setter method references.
	 *
	 * <pre>{@code
	 * new FilterOmit(User::setPassword, User::setSecret)
	 * }</pre>
	 *
	 * @param <T> the entity type
	 * @param setters setter method references
	 */
	@SafeVarargs
	public <T> FilterOmit(final SerializableBiConsumer<T, ?>... setters) {
		final List<String> resolved = new ArrayList<>(setters.length);
		for (final SerializableBiConsumer<T, ?> setter : setters) {
			resolved.add(MethodReferenceResolver.resolveFieldName(setter));
		}
		this.filterValue = resolved;
	}

	/**
	 * Create a FilterOmit from fluent setter method references.
	 *
	 * @param <T> the entity type
	 * @param setters fluent setter method references
	 */
	@SafeVarargs
	public <T> FilterOmit(final SerializableBiFunction<T, ?, ?>... setters) {
		final List<String> resolved = new ArrayList<>(setters.length);
		for (final SerializableBiFunction<T, ?, ?> setter : setters) {
			resolved.add(MethodReferenceResolver.resolveFieldName(setter));
		}
		this.filterValue = resolved;
	}

	/**
	 * Create a FilterOmit from mixed field references (getter, void setter, fluent setter).
	 *
	 * @param <T> the entity type
	 * @param refs field references
	 */
	@SafeVarargs
	public <T> FilterOmit(final FieldRef<T>... refs) {
		final List<String> resolved = new ArrayList<>(refs.length);
		for (final FieldRef<T> ref : refs) {
			resolved.add(ref.getFieldName());
		}
		this.filterValue = resolved;
	}

	/**
	 * Returns the list of field names to exclude.
	 *
	 * @return the list of excluded field names
	 */
	public List<String> getValues() {
		return this.filterValue;
	}
}
