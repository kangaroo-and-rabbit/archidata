package org.atriasoft.archidata.dataAccess;

import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class QueryInList<T> implements QueryItem {
	protected final String key;
	protected final String comparator;
	protected final List<T> value;

	protected QueryInList(final String key, final String comparator, final List<T> value) {
		this.key = key;
		this.comparator = comparator;
		this.value = value;
	}

	public QueryInList(final String key, final List<T> value) {
		this(key, "IN", value);
	}

	public QueryInList(final String key, @SuppressWarnings("unchecked") final T... value) {
		this(key, "IN", List.of(value));
	}

	@Override
	public void generateFilter(final List<Bson> filters) {
		filters.add(Filters.in(this.key, this.value));
	}
}
