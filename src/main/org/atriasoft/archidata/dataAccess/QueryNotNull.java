package org.atriasoft.archidata.dataAccess;

import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class QueryNotNull implements QueryItem {
	private final String key;

	public QueryNotNull(final String key) {
		this.key = key;
	}

	@Override
	public void generateFilter(final List<Bson> filters) {
		filters.add(Filters.exists(this.key));
	}
}
