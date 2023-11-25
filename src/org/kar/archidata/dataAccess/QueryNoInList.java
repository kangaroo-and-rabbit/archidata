package org.kar.archidata.dataAccess;

import java.util.List;

public class QueryNoInList<T> extends QueryInList<T> {
	public QueryNoInList(final String key, final List<T> value) {
		super(key, "NOT IN", value);
	}
}
