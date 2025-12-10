package org.atriasoft.archidata.dataAccess;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

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
