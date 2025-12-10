package org.atriasoft.archidata.dataAccess.options;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.QueryItem;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public class Condition extends QueryOption {
	public final QueryItem condition;

	public Condition(final QueryItem items) {
		this.condition = items;
	}

	public Condition() {
		this.condition = null;
	}

	public Bson getFilter(final String collectionName, final QueryOptions options, final String deletedFieldName) {
		boolean exclude_deleted = true;
		if (options != null) {
			exclude_deleted = !options.exist(AccessDeletedItems.class);
		}
		final List<Bson> filter = new ArrayList<>();
		if (exclude_deleted && deletedFieldName != null) {
			filter.add(Filters.or(Filters.eq(deletedFieldName, false), Filters.exists(deletedFieldName, false)));
		}
		// Check if we have a condition to generate
		if (this.condition != null) {
			this.condition.generateFilter(filter);
		}
		if (filter.size() == 0) {
			return null;
		}
		if (filter.size() == 1) {
			return filter.get(0);
		}
		return Filters.and(filter.toArray(new Bson[0]));
	}
}
