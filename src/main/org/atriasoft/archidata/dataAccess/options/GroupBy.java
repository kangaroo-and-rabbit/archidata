package org.atriasoft.archidata.dataAccess.options;

import java.sql.PreparedStatement;
import java.util.List;

import org.atriasoft.archidata.dataAccess.CountInOut;

public class GroupBy extends QueryOption {
	protected final List<String> childs;

	public GroupBy(final List<String> childs) {
		this.childs = childs;
	}

	public GroupBy(final String... childs) {
		this.childs = List.of(childs);
	}

	public void generateQuery(final StringBuilder query, final String tableName) {
		if (this.childs.size() == 0) {
			return;
		}
		query.append(" GROUP BY ");
		boolean first = true;
		for (final String elem : this.childs) {
			if (first) {
				first = false;
			} else {
				query.append(", ");
			}
			// query.append("`");
			query.append(elem);
			// query.append("` ");
		}
		query.append("\n");
	}

	public void injectQuery(final PreparedStatement ps, final CountInOut iii) throws Exception {
		// nothing to add.
	}
}
