package org.atriasoft.archidata.dataAccess.options;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

/**
 * Condition option for filtering database queries.
 *
 * <p>
 * Uses MongoDB BSON Filters for defining conditions.
 * </p>
 *
 * <strong>Examples:</strong>
 * <pre>{@code
 * // Simple filter
 * DataAccess.gets(User.class, new Condition(Filters.gt("age", 18)));
 *
 * // Complex filter with multiple conditions
 * DataAccess.gets(User.class, new Condition(Filters.and(
 *     Filters.gt("age", 18),
 *     Filters.lt("age", 65),
 *     Filters.eq("active", true)
 * )));
 *
 * // Using MongoDB operators
 * DataAccess.gets(User.class, new Condition(Filters.in("role", "admin", "moderator")));
 * DataAccess.gets(User.class, new Condition(Filters.regex("email", ".*@example.com")));
 * }</pre>
 */
public class Condition extends QueryOption {
	private final Bson bsonFilter;

	/**
	 * Create a Condition with a BSON filter.
	 *
	 * <p>Example:</p>
	 * <pre>
	 * new Condition(Filters.gt("age", 18))
	 * </pre>
	 *
	 * @param bsonFilter The BSON filter to use
	 */
	public Condition(final Bson bsonFilter) {
		this.bsonFilter = bsonFilter;
	}

	/**
	 * Create an empty Condition (no filter).
	 */
	public Condition() {
		this.bsonFilter = null;
	}

	public Bson getFilter() {
		return this.bsonFilter;
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
		if (this.bsonFilter != null) {
			filter.add(this.bsonFilter);
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
