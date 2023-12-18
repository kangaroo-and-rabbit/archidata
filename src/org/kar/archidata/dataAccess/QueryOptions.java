package org.kar.archidata.dataAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kar.archidata.dataAccess.options.AccessDeletedItems;
import org.kar.archidata.dataAccess.options.CreateDropTable;
import org.kar.archidata.dataAccess.options.ReadAllColumn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryOptions {
	static final Logger LOGGER = LoggerFactory.getLogger(QueryOptions.class);
	public static final ReadAllColumn READ_ALL_COLOMN = new ReadAllColumn();
	public static final AccessDeletedItems ACCESS_DELETED_ITEMS = new AccessDeletedItems();
	public static final CreateDropTable CREATE_DROP_TABLE = new CreateDropTable();

	private final List<QueryOption> options = new ArrayList<>();

	public QueryOptions(final QueryOption... elems) {
		Collections.addAll(this.options, elems);
	}

	public QueryOptions() {}

	public void add(final QueryOption option) {
		this.options.add(option);
	}

	public List<QueryOption> getAll() {
		return this.options;
	}

	public QueryOption[] getAllArray() {
		return this.options.toArray(new QueryOption[0]);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(final Class<T> type) {
		for (final QueryOption elem : this.options) {
			if (elem.getClass() == type) {
				return (T) elem;
			}
		}
		return null;
	}

	public boolean exist(final Class<?> type) {
		for (final QueryOption elem : this.options) {
			if (elem.getClass() == type) {
				return true;
			}
		}
		return false;
	}

	public static boolean readAllColomn(final QueryOptions options) {
		if (options != null) {
			return options.exist(ReadAllColumn.class);
		}
		return false;
	}

}
