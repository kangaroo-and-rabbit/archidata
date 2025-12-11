package org.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

public class QueryAnd implements QueryItem {
	protected final List<Bson> children;
	protected Bson filter;

	public QueryAnd() {
		this.children = new ArrayList<>();
	}

	public QueryAnd(final List<QueryItem> child) {
		this.children = child.stream().map(QueryItem::getFilter).filter(filter -> filter != null).toList();
		updateFilter();
	}

	public QueryAnd(final QueryItem... child) {
		this.children = List.of(child).stream().map(QueryItem::getFilter).filter(filter -> filter != null).toList();
		updateFilter();
	}

	public QueryAnd(final List<Bson> bsonFilters, final boolean isBson) {
		this.children = new ArrayList<>(bsonFilters);
		updateFilter();
	}

	public QueryAnd(final Bson... bsonFilters) {
		this.children = new ArrayList<>();
		Collections.addAll(this.children, bsonFilters);
		updateFilter();
	}

	public void add(final QueryItem... child) {
		List.of(child).stream()
				.map(QueryItem::getFilter)
				.filter(filter -> filter != null)
				.forEach(this.children::add);
		updateFilter();
	}

	public void add(final Bson... bsonFilters) {
		Collections.addAll(this.children, bsonFilters);
		updateFilter();
	}

	public int size() {
		return this.children.size();
	}

	protected void updateFilter() {
		this.filter = Filters.and(this.children.toArray(new Bson[0]));
	}

	@Override
	public Bson getFilter() {
		return this.filter;
	}
}
