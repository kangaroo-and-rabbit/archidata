package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;
import java.util.List;

public class QueryOr implements QueryItem {
	protected final List<QueryItem> childs;

	public QueryOr(final List<QueryItem> childs) {
		this.childs = childs;
	}

	@Override
	public void generateQuerry(final StringBuilder querry, final String tableName) {
		if (this.childs.size() >= 1) {
			querry.append(" (");
		}
		boolean first = true;
		for (final QueryItem elem : this.childs) {
			if (first) {
				first = false;
			} else {
				querry.append(" OR ");
			}
			elem.generateQuerry(querry, tableName);
		}
		if (this.childs.size() >= 1) {
			querry.append(")");
		}
	}

	@Override
	public void injectQuerry(final PreparedStatement ps, final CountInOut iii) throws Exception {
		for (final QueryItem elem : this.childs) {
			elem.injectQuerry(ps, iii);
		}
	}
}
