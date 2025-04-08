package org.atriasoft.archidata.dataAccess;

import java.sql.PreparedStatement;
import java.util.List;

import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.Filters;

public class QueryCondition implements QueryItem {
	static final Logger LOGGER = LoggerFactory.getLogger(DBAccess.class);
	private final String key;
	private final String comparator;
	private final Object value;

	/**
	 * Simple DB comparison element. Note the injected object is injected in the statement and not in the query directly.
	 * @param key Field to check (the Model property name)
	 * @param comparator (simple comparator String)
	 * @param value Value that the field must be equals.
	 */
	public QueryCondition(final String key, final String comparator, final Object value) {
		this.key = key;
		this.comparator = comparator;
		this.value = value;
	}

	@Override
	public void generateQuery(final StringBuilder query, final String tableName) {
		if (tableName != null) {
			query.append(tableName);
			query.append(".");
		}
		query.append(this.key);
		query.append(" ");
		query.append(this.comparator);
		query.append(" ?");
	}

	@Override
	public void injectQuery(final DBAccessSQL ioDb, final PreparedStatement ps, final CountInOut iii) throws Exception {
		ioDb.addElement(ps, this.value, iii);
		iii.inc();
	}

	@Override
	public void generateFilter(final List<Bson> filters) {
		if ("=".equals(this.comparator)) {
			filters.add(Filters.eq(this.key, this.value));
		} else if ("!=".equals(this.comparator)) {
			filters.add(Filters.ne(this.key, this.value));
		} else if (">".equals(this.comparator)) {
			filters.add(Filters.gt(this.key, this.value));
		} else if (">=".equals(this.comparator)) {
			filters.add(Filters.gte(this.key, this.value));
		} else if ("<".equals(this.comparator)) {
			filters.add(Filters.lt(this.key, this.value));
		} else if ("<=".equals(this.comparator)) {
			filters.add(Filters.lte(this.key, this.value));
		} else {
			LOGGER.error("Not manage comparison: '{}'", this.key);
		}

	}
}
