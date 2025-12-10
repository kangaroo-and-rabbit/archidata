package org.atriasoft.archidata.dataAccess;

import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class QueryNoInList<T> implements QueryItem {
	protected final Bson filter;

	protected QueryNoInList(final String key, final List<T> value) {
		this.filter = Filters.nin(key, value);
	}

	public QueryNoInList(final String key, @SuppressWarnings("unchecked") final T... value) {
		this(key, List.of(value));
	}

	@Override
	public Bson getFilter() {
		return this.filter;
	}
}
