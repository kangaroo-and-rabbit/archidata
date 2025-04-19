package org.atriasoft.archidata.dataAccess;

import java.sql.PreparedStatement;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class QueryNull implements QueryItem {
	private final String key;

	public QueryNull(final String key) {
		this.key = key;
	}

	@Override
	public void generateQuery(final StringBuilder query, final String tableName) {
		if (tableName != null) {
			query.append(tableName);
			query.append(".");
		}
		query.append(this.key);
		query.append(" IS NULL");
	}

	@Override
	public void injectQuery(final DBAccessSQL ioDb, final PreparedStatement ps, final CountInOut iii)
			throws Exception {}

	@Override
	public void generateFilter(final List<Bson> filters) {
		// Not sure of the result ... maybe check it ...
		filters.add(Filters.eq(this.key, null));
	}
}
