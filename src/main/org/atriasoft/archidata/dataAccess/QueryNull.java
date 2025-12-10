package org.atriasoft.archidata.dataAccess;

import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class QueryNull implements QueryItem {
	private final String key;

	public QueryNull(final String key) {
		this.key = key;
	}

	@Override
	public void generateFilter(final List<Bson> filters) {
		// Not sure of the result ... maybe check it ...
		filters.add(Filters.eq(this.key, null));
	}
}
