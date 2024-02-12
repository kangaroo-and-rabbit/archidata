package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;

public class QueryCondition implements QueryItem {
	private final String key;
	private final String comparator;
	private final Object value;

	public QueryCondition(final String key, final String comparator, final Object value) {
		this.key = key;
		this.comparator = comparator;
		this.value = value;
	}

	@Override
	public void generateQuerry(final StringBuilder query, final String tableName) {
		if (tableName != null) {
			query.append(tableName);
			query.append(".");
		}
		query.append(this.key);
		query.append(" ");
		query.append(this.comparator);
		query.append(" ?");
	}

	@Override
	public void injectQuerry(final PreparedStatement ps, final CountInOut iii) throws Exception {
		DataAccess.addElement(ps, this.value, iii);
		iii.inc();
	}
}
