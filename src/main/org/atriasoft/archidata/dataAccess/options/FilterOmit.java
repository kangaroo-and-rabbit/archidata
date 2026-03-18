package org.atriasoft.archidata.dataAccess.options;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.MethodReferenceResolver;
import org.atriasoft.archidata.dataAccess.SerializableBiConsumer;
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
	public final List<String> filterValue;

	public FilterOmit(final List<String> filterValue) {
		this.filterValue = filterValue;
	}

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

	public List<String> getValues() {
		return this.filterValue;
	}
}
