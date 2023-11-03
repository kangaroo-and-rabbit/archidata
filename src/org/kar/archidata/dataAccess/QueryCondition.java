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
	public void generateQuerry(final StringBuilder querry, final String tableName) {
		querry.append(tableName);
		querry.append(".");
		querry.append(this.key);
		querry.append(" ");
		querry.append(this.comparator);
		querry.append(" ?");

	}

	@Override
	public void injectQuerry(final PreparedStatement ps, final CountInOut iii) throws Exception {
		DataAccess.addElement(ps, this.value, iii);
		iii.inc();
	}
}
