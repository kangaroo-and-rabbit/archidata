package org.atriasoft.archidata.dataAccess;

import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class QueryExist implements QueryItem {
	private final String key;
	private final boolean exist;

	public QueryExist(final String key, final boolean exist) {
		this.key = key;
		this.exist = exist;
	}

	@Override
	public void generateQuery(final StringBuilder query, final String tableName) {
		// not implemented
	}

	@Override
	public void generateFilter(final List<Bson> filters) {
		filters.add(Filters.exists(this.key, this.exist));
	}
}
