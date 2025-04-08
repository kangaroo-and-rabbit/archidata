package org.atriasoft.archidata.dataAccess;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class QueryAnd implements QueryItem {
	protected final List<QueryItem> childs;

	public QueryAnd(final List<QueryItem> child) {
		this.childs = child;
	}

	public QueryAnd(final QueryItem... child) {
		this.childs = new ArrayList<>();
		Collections.addAll(this.childs, child);
	}

	public void add(final QueryItem... child) {
		Collections.addAll(this.childs, child);
	}

	@Override
	public void generateQuery(final StringBuilder query, final String tableName) {
		if (this.childs.size() >= 1) {
			query.append(" (");
		}
		boolean first = true;
		for (final QueryItem elem : this.childs) {
			if (first) {
				first = false;
			} else {
				query.append(" AND ");
			}
			elem.generateQuery(query, tableName);
		}
		if (this.childs.size() >= 1) {
			query.append(")");
		}
	}

	@Override
	public void injectQuery(final DBAccessSQL ioDb, final PreparedStatement ps, final CountInOut iii) throws Exception {
		for (final QueryItem elem : this.childs) {
			elem.injectQuery(ioDb, ps, iii);
		}
	}

	public int size() {
		return this.childs.size();
	}

	@Override
	public void generateFilter(final List<Bson> filters) {
		final List<Bson> filtersLocal = new ArrayList<>();
		for (final QueryItem elem : this.childs) {
			elem.generateFilter(filtersLocal);
		}
		filters.add(Filters.and(filtersLocal.toArray(new Bson[0])));
	}
}
