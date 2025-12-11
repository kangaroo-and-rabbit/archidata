package org.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

// Note the query Item is deprecated soon, please use Filter.xxx() instead
public class QueryOr implements QueryItem {
	protected final Bson filter;

	public QueryOr(final List<QueryItem> children) {
		final List<Bson> filtersLocal = new ArrayList<>();
		for (final QueryItem child : children) {
			final Bson filter = child.getFilter();
			if (filter != null) {
				filtersLocal.add(filter);
			}
		}
		this.filter = Filters.or(filtersLocal.toArray(new Bson[0]));
	}

	public QueryOr(final QueryItem... children) {
		this(List.of(children));
	}

	@Override
	public Bson getFilter() {
		return this.filter;
	}
}
