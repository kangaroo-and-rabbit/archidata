package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;
import java.util.List;

public class QueryInList<T> implements QueryItem {
	protected final String key;
	protected final String comparator;
	protected final List<T> value;

	protected QueryInList(final String key, final String comparator, final List<T> value) {
		this.key = key;
		this.comparator = comparator;
		this.value = value;
	}

	public QueryInList(final String key, final List<T> value) {
		this(key, "IN", value);
	}

	public QueryInList(final String key, final T... value) {
		this(key, "IN", List.of(value));
	}

	@Override
	public void generateQuerry(final StringBuilder query, final String tableName) {
		query.append(tableName);
		query.append(".");
		query.append(this.key);
		query.append(" ");
		query.append(this.comparator);
		query.append(" (");
		for (int iii = 0; iii < this.value.size(); iii++) {
			if (iii != 0) {
				query.append(",?");
			} else {
				query.append("?");
			}
		}
		query.append(")");

	}

	@Override
	public void injectQuerry(final PreparedStatement ps, final CountInOut iii) throws Exception {
		for (final Object elem : this.value) {
			DataAccess.addElement(ps, elem, iii);
			iii.inc();
		}
	}
}
