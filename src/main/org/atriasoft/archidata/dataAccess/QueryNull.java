package org.atriasoft.archidata.dataAccess;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

// Note the query Item is deprecated soon, please use Filter.xxx() instead
public class QueryNull implements QueryItem {
	private final Bson filter;

	public QueryNull(final String key) {
		this.filter = Filters.eq(key, null);
	}

	@Override
	public Bson getFilter() {
		return this.filter;
	}
}
