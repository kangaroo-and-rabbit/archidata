package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;

public class QueryCondition implements QueryItem {
	private final String key;
	private final String comparator;
	private final Object value;

	/**
	 * Simple DB comparison element. Note the injected object is injected in the statement and not in the query directly.
	 * @param key Field to check (the Model property name)
	 * @param comparator (simple comparator String)
	 * @param value Value that the field must be equals.
	 */
	public QueryCondition(final String key, final String comparator, final Object value) {
		this.key = key;
		this.comparator = comparator;
		this.value = value;
	}

	@Override
	public void generateQuery(final StringBuilder query, final String tableName) {
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
	public void injectQuery(final PreparedStatement ps, final CountInOut iii) throws Exception {
		DataAccess.addElement(ps, this.value, iii);
		iii.inc();
	}
}
