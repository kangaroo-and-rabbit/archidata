package org.atriasoft.archidata.dataAccess.options;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.bson.conversions.Bson;

import org.atriasoft.archidata.dataAccess.Filters;

/**
 * Condition option for filtering database queries.
 *
 * <p>
 * Uses {@link Filters} for defining conditions. Supports both string-based
 * field names and type-safe method references.
 * </p>
 *
 * <strong>String-based examples:</strong>
 * <pre>{@code
 * DataAccess.gets(User.class, new Condition(Filters.gt("age", 18)));
 * DataAccess.gets(User.class, new Condition(Filters.in("role", "admin", "moderator")));
 * }</pre>
 *
 * <strong>Type-safe method reference examples:</strong>
 * <pre>{@code
 * DataAccess.gets(User.class, new Condition(Filters.gt(User::getAge, 18)));
 * DataAccess.gets(User.class, new Condition(Filters.eq(User::isActive, true)));
 * DataAccess.gets(User.class, new Condition(Filters.and(
 *     Filters.gt(User::getAge, 18),
 *     Filters.eq(User::isActive, true)
 * )));
 * }</pre>
 *
 * @see Filters
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
