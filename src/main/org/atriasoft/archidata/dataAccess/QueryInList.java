package org.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

// Note the query Item is deprecated soon, please use Filter.xxx() instead
public class QueryInList<T> implements QueryItem {
	protected final Bson filter;

	protected QueryInList(final String key, final List<T> value) {
		this.filter = Filters.in(key, value);
	}

	public QueryInList(final String key, @SuppressWarnings("unchecked") final T... value) {
		// Detect if a collection was passed as varargs (which creates an array with a single element)
		if (value.length == 1 && value[0] instanceof Collection) {
			this.filter = Filters.in(key, (Collection<?>) value[0]);
		} else {
			this.filter = Filters.in(key, value);
		}
	}

	@Override
	public Bson getFilter() {
		return this.filter;
	}
}
