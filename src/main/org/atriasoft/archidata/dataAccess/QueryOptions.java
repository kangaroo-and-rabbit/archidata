package org.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Note the query Item is deprecated soon, please use Filter.xxx() instead
public class QueryOptions {
	static final Logger LOGGER = LoggerFactory.getLogger(QueryOptions.class);

	private final List<QueryOption> options = new ArrayList<>();

	public QueryOptions() {}

	public QueryOptions(final QueryOption... elems) {
		if (elems == null || elems.length == 0) {
			return;
		}
		for (final QueryOption elem : elems) {
			add(elem);
		}

	}

	public void add(final QueryOption option) {
		if (option == null) {
			return;
		}
		this.options.add(option);
	}

	public List<QueryOption> getAll() {
		return this.options;
	}

	public QueryOption[] getAllArray() {
		return this.options.toArray(new QueryOption[0]);
	}

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

	public boolean exist(final Class<?> type) {
		for (final QueryOption elem : this.options) {
			if (elem.getClass() == type) {
				return true;
			}
		}
		return false;
	}

	public static boolean readAllColumn(final QueryOptions options) {
		if (options != null) {
			return options.exist(ReadAllColumn.class);
		}
		return false;
	}
}
