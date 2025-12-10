package org.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class QueryOr implements QueryItem {
	protected final List<QueryItem> childs;

	public QueryOr(final List<QueryItem> childs) {
		this.childs = childs;
	}

	public QueryOr(final QueryItem... childs) {
		this.childs = List.of(childs);
	}

	@Override
	public void generateFilter(final List<Bson> filters) {
		final List<Bson> filtersLocal = new ArrayList<>();
		for (final QueryItem elem : this.childs) {
			elem.generateFilter(filtersLocal);
		}
		filters.add(Filters.or(filtersLocal.toArray(new Bson[0])));
	}
}
