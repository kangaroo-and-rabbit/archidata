package org.atriasoft.archidata.dataAccess;

import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.Filters;

public class QueryCondition implements QueryItem {
	static final Logger LOGGER = LoggerFactory.getLogger(DBAccessMongo.class);
	private final Bson filter;

	/**
	 * Simple DB comparison element. Note the injected object is injected in the statement and not in the query directly.
	 * @param key Field to check (the Model property name)
	 * @param comparator (simple comparator String)
	 * @param value Value that the field must be equals.
	 */
	public QueryCondition(final String key, final String comparator, final Object value) {
		if ("=".equals(comparator)) {
			this.filter = Filters.eq(key, value);
		} else if ("!=".equals(comparator)) {
			this.filter = Filters.ne(key, value);
		} else if (">".equals(comparator)) {
			this.filter = Filters.gt(key, value);
		} else if (">=".equals(comparator)) {
			this.filter = Filters.gte(key, value);
		} else if ("<".equals(comparator)) {
			this.filter = Filters.lt(key, value);
		} else if ("<=".equals(comparator)) {
			this.filter = Filters.lte(key, value);
		} else {
			LOGGER.error("Not manage comparison: '{}' for key '{}'", comparator, key);
			this.filter = null;
		}
	}

	@Override
	public Bson getFilter() {
		return this.filter;
	}

}
