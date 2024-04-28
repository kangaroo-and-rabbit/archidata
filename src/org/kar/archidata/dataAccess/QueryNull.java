package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;

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
	public void injectQuery(final PreparedStatement ps, final CountInOut iii) throws Exception {}
}
