package org.atriasoft.archidata.dataAccess;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class QueryExist implements QueryItem {
	private final Bson filter;

	public QueryExist(final String key, final boolean exist) {
		this.filter = Filters.exists(key, exist);
	}

	@Override
	public Bson getFilter() {
		return this.filter;
	}
}
