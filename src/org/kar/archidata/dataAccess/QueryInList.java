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

	@Override
	public void generateQuerry(final StringBuilder querry, final String tableName) {
		querry.append(tableName);
		querry.append(".");
		querry.append(this.key);
		querry.append(" ");
		querry.append(this.comparator);
		querry.append(" (");
		for (int iii = 0; iii < this.value.size(); iii++) {
			if (iii != 0) {
				querry.append(",?");
			} else {
				querry.append("?");
			}
		}
		querry.append(")");

	}

	@Override
	public void injectQuerry(final PreparedStatement ps, final CountInOut iii) throws Exception {
		for (final Object elem : this.value) {
			DataAccess.addElement(ps, elem, iii);
			iii.inc();
		}
	}
}
