package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;
import java.util.List;

public class OrderBy extends QueryOption {
	protected final List<OrderItem> childs;

	public OrderBy(final List<OrderItem> childs) {
		this.childs = childs;
	}

	public OrderBy(final OrderItem... childs) {
		this.childs = List.of(childs);
	}

	public void generateQuerry(final StringBuilder query, final String tableName) {
		if (this.childs.size() >= 1) {
			query.append(" ORDER BY ");
		}
		boolean first = true;
		for (final OrderItem elem : this.childs) {
			if (first) {
				first = false;
			} else {
				query.append(", ");
			}
			query.append("`");
			query.append(elem.value);
			query.append("` ");
			query.append(elem.order.toString());
		}
		if (this.childs.size() >= 1) {
			query.append(")");
		}
	}

	public void injectQuerry(final PreparedStatement ps, final CountInOut iii) throws Exception {
		// nothing to add.
	}
}
