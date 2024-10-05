package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class QueryNotNull implements QueryItem {
	private final String key;

	public QueryNotNull(final String key) {
		this.key = key;
	}

	@Override
	public void generateQuery(final StringBuilder query, final String tableName) {
		if (tableName != null) {
			query.append(tableName);
			query.append(".");
		}
		query.append(this.key);
		query.append(" IS NOT NULL");
	}

	@Override
	public void injectQuery(final PreparedStatement ps, final CountInOut iii) throws Exception {}

	@Override
	public void generateFilter(final List<Bson> filters) {
		filters.add(Filters.exists(this.key));
	}
}
