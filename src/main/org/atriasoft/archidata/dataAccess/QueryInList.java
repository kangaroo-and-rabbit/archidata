package org.atriasoft.archidata.dataAccess;

import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class QueryInList<T> implements QueryItem {
	protected final Bson filter;

	protected QueryInList(final String key, final List<T> value) {
		this.filter = Filters.in(key, value.toArray());
	}

	public QueryInList(final String key, @SuppressWarnings("unchecked") final T... value) {
		this.filter = Filters.in(key, value);
	}

	@Override
	public Bson getFilter() {
		return this.filter;
	}
}
