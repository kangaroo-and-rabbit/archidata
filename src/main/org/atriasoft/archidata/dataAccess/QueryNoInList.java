package org.atriasoft.archidata.dataAccess;

import java.util.Collection;
import java.util.List;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

// Note the query Item is deprecated soon, please use Filter.xxx() instead
public class QueryNoInList<T> implements QueryItem {
	protected final Bson filter;

	protected QueryNoInList(final String key, final List<T> value) {
		this.filter = Filters.nin(key, value);
	}

	public QueryNoInList(final String key, @SuppressWarnings("unchecked") final T... value) {
		// Detect if a collection was passed as varargs (which creates an array with a single element)
		if (value.length == 1 && value[0] instanceof Collection) {
			this.filter = Filters.nin(key, (Collection<?>) value[0]);
		} else {
			this.filter = Filters.nin(key, value);
		}
	}

	@Override
	public Bson getFilter() {
		return this.filter;
	}
}
