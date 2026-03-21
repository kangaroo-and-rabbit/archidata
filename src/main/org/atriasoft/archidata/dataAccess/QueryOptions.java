package org.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container for query options that control how database operations are executed.
 *
 * <p>Collects {@link QueryOption} instances such as
 * {@link org.atriasoft.archidata.dataAccess.options.Condition},
 * {@link org.atriasoft.archidata.dataAccess.options.FilterValue},
 * {@link org.atriasoft.archidata.dataAccess.options.Limit}, and
 * {@link org.atriasoft.archidata.dataAccess.options.OrderBy}.
 */
public class QueryOptions {
	static final Logger LOGGER = LoggerFactory.getLogger(QueryOptions.class);

	private final List<QueryOption> options = new ArrayList<>();

	/** Creates an empty QueryOptions container. */
	public QueryOptions() {}

	/**
	 * Creates a QueryOptions container initialized with the given options.
	 *
	 * @param elems The query options to add (may be null or empty)
	 */
	public QueryOptions(final QueryOption... elems) {
		if (elems == null || elems.length == 0) {
			return;
		}
		for (final QueryOption elem : elems) {
			add(elem);
		}

	}

	/**
	 * Adds a query option to this container.
	 *
	 * @param option The option to add (null values are ignored)
	 */
	public void add(final QueryOption option) {
		if (option == null) {
			return;
		}
		this.options.add(option);
	}

	/**
	 * Returns all query options in this container.
	 *
	 * @return The list of all stored query options
	 */
	public List<QueryOption> getAll() {
		return this.options;
	}

	/**
	 * Returns all query options as an array.
	 *
	 * @return An array containing all stored query options
	 */
	public QueryOption[] getAllArray() {
		return this.options.toArray(new QueryOption[0]);
	}

	/**
	 * Returns all query options of the specified type.
	 *
	 * @param <T>  The type of options to retrieve
	 * @param type The class of the option type to filter by
	 * @return A list of matching options (empty if none found)
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> get(final Class<T> type) {
		final List<T> out = new ArrayList<>();
		for (final QueryOption elem : this.options) {
			if (elem.getClass() == type) {
				out.add((T) elem);
			}
		}
		return out;
	}

	/**
	 * Checks whether at least one option of the specified type exists.
	 *
	 * @param type The class of the option type to check for
	 * @return true if at least one option of the given type exists
	 */
	public boolean exist(final Class<?> type) {
		for (final QueryOption elem : this.options) {
			if (elem.getClass() == type) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks whether the given options request reading all columns.
	 *
	 * @param options The query options to check (may be null)
	 * @return true if a {@link ReadAllColumn} option is present
	 */
	public static boolean readAllColumn(final QueryOptions options) {
		if (options != null) {
			return options.exist(ReadAllColumn.class);
		}
		return false;
	}
}
