package org.atriasoft.archidata.dataAccess.statistic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.bson.Document;
import org.bson.conversions.Bson;

/**
 * Normalized shape of a database query, with the values stripped out.
 *
 * <p>
 * Two queries that only differ by the <i>values</i> they filter on produce the very same signature:
 * {@code Filters.eq("name", "bob")} and {@code Filters.eq("name", "alice")} both collapse to
 * {@code E:name}. That is exactly what an index is about — an index is built on field
 * <i>names</i>, never on values — so counting signatures counts "how many times would this index
 * have been used".
 * </p>
 *
 * <p>
 * The fields are split following the MongoDB <b>ESR rule</b> (Equality, Sort, Range), which is the
 * order a compound index must follow to be fully usable:
 * </p>
 * <ul>
 * <li><b>E</b>quality: fields tested with an exact match ({@code $eq}, {@code $in}, {@code $all}).
 * Their relative order inside the index is irrelevant, so they are sorted alphabetically to make
 * the signature stable.</li>
 * <li><b>S</b>ort: the fields of the {@code sort()} clause, kept <b>in their original order</b>
 * (and with their direction), because that order is significant.</li>
 * <li><b>R</b>ange: fields tested with a range predicate ({@code $gt}, {@code $lt}, {@code $ne},
 * {@code $regex}, {@code $exists}, ...). Sorted alphabetically as well.</li>
 * </ul>
 *
 * <p>
 * A field carrying both an equality and a range predicate is classified as a <b>range</b> field:
 * that is the constraint that dictates its position in the index.
 * </p>
 *
 * @see QueryStatistics
 */
public final class QuerySignature {

	/** Operators that keep a field in the "Equality" group of the ESR rule. */
	private static final Set<String> EQUALITY_OPERATORS = Set.of("$eq", "$in", "$all");

	/** The kind of database operation that carried the filter. */
	public enum Operation {
		/** A read: {@code get}, {@code gets}, {@code getById}, {@code getAll}, {@code existsById}. */
		FIND,
		/** A {@code countDocuments}. */
		COUNT,
		/** An {@code updateMany}. */
		UPDATE,
		/** A hard {@code deleteMany}. */
		DELETE,
		/** A soft delete (an {@code updateMany} flagging the documents as deleted). */
		DELETE_SOFT,
		/** A restore of soft-deleted documents. */
		RESTORE
	}

	private final String collection;
	private final Operation operation;
	private final List<String> equality;
	private final List<String> sort;
	private final List<String> range;
	private final String softDeleteField;
	private final boolean containsOr;
	private final String key;

	private QuerySignature(final String collection, final Operation operation, final List<String> equality,
			final List<String> sort, final List<String> range, final String softDeleteField, final boolean containsOr) {
		this.collection = collection;
		this.operation = operation;
		this.equality = List.copyOf(equality);
		this.sort = List.copyOf(sort);
		this.range = List.copyOf(range);
		this.softDeleteField = softDeleteField;
		this.containsOr = containsOr;
		this.key = collection + "|" + operation + "|E:" + String.join(",", this.equality) + "|S:"
				+ String.join(",", this.sort) + "|R:" + String.join(",", this.range);
	}

	/**
	 * Extracts the signature of a query.
	 *
	 * @param collection the collection the query runs on
	 * @param operation the kind of operation
	 * @param filter the user filter, <b>before</b> the soft-delete exclusion is merged in, or
	 *        {@code null} when the query has no filter at all
	 * @param sort the sort document ({@code field -> 1|-1}, in order), or {@code null} when the
	 *        query is unsorted
	 * @param softDeleteField the soft-delete field implicitly excluded by the query, or {@code null}
	 *        when the query does not exclude soft-deleted documents
	 * @return the normalized signature of the query
	 */
	public static QuerySignature of(
			final String collection,
			final Operation operation,
			final Bson filter,
			final Document sort,
			final String softDeleteField) {
		final Set<String> equality = new TreeSet<>();
		final Set<String> range = new TreeSet<>();
		final boolean[] containsOr = { false };
		if (filter != null) {
			walk(filter.toBsonDocument(), equality, range, containsOr);
		}
		// A range predicate always wins: it is what pins the field at the tail of the index.
		equality.removeAll(range);
		final List<String> sortFields = new ArrayList<>();
		if (sort != null) {
			for (final Map.Entry<String, Object> entry : sort.entrySet()) {
				final Object direction = entry.getValue();
				final boolean descending = direction instanceof final Number number && number.intValue() < 0;
				sortFields.add(entry.getKey() + ":" + (descending ? "-1" : "1"));
			}
		}
		return new QuerySignature(collection, operation, new ArrayList<>(equality), sortFields, new ArrayList<>(range),
				softDeleteField, containsOr[0]);
	}

	/**
	 * Recursively walks a BSON filter and dispatches every field it meets into the equality or the
	 * range bucket.
	 *
	 * @param doc the (sub-)filter to walk
	 * @param equality collects the fields matched by equality
	 * @param range collects the fields matched by a range predicate
	 * @param containsOr single-cell flag raised when a {@code $or}/{@code $nor} is met
	 */
	private static void walk(
			final BsonDocument doc,
			final Set<String> equality,
			final Set<String> range,
			final boolean[] containsOr) {
		for (final Map.Entry<String, BsonValue> entry : doc.entrySet()) {
			final String key = entry.getKey();
			final BsonValue value = entry.getValue();
			switch (key) {
				case "$and" -> walkArray(value, equality, range, containsOr);
				case "$or", "$nor" -> {
					containsOr[0] = true;
					walkArray(value, equality, range, containsOr);
				}
				case "$not" -> {
					if (value.isDocument()) {
						walk(value.asDocument(), equality, range, containsOr);
					}
				}
				default -> {
					if (key.startsWith("$")) {
						// $expr, $where, $text, $comment... : not tied to an indexable field name.
						continue;
					}
					classify(key, value, equality, range, containsOr);
				}
			}
		}
	}

	/**
	 * Walks every document of a BSON array of sub-filters.
	 *
	 * @param value the array holding the sub-filters
	 * @param equality collects the fields matched by equality
	 * @param range collects the fields matched by a range predicate
	 * @param containsOr single-cell flag raised when a {@code $or}/{@code $nor} is met
	 */
	private static void walkArray(
			final BsonValue value,
			final Set<String> equality,
			final Set<String> range,
			final boolean[] containsOr) {
		if (!value.isArray()) {
			return;
		}
		for (final BsonValue element : value.asArray()) {
			if (element.isDocument()) {
				walk(element.asDocument(), equality, range, containsOr);
			}
		}
	}

	/**
	 * Classifies a single {@code field: predicate} pair into the equality or the range bucket.
	 *
	 * @param field the field name
	 * @param value the predicate applied to that field
	 * @param equality collects the fields matched by equality
	 * @param range collects the fields matched by a range predicate
	 * @param containsOr single-cell flag raised when a {@code $or}/{@code $nor} is met
	 */
	private static void classify(
			final String field,
			final BsonValue value,
			final Set<String> equality,
			final Set<String> range,
			final boolean[] containsOr) {
		if (!value.isDocument()) {
			// Implicit $eq: {name: "bob"}
			equality.add(field);
			return;
		}
		final BsonDocument doc = value.asDocument();
		final boolean onlyOperators = !doc.isEmpty() && doc.keySet().stream().allMatch(it -> it.startsWith("$"));
		if (!onlyOperators) {
			// Exact match on a whole sub-document: {address: {city: "paris"}}
			equality.add(field);
			return;
		}
		for (final Map.Entry<String, BsonValue> entry : doc.entrySet()) {
			final String operator = entry.getKey();
			if (EQUALITY_OPERATORS.contains(operator)) {
				equality.add(field);
			} else if ("$elemMatch".equals(operator)) {
				// The sub-conditions apply to the elements of the array: report them as
				// "field.subField" so the suggested index is directly usable.
				final Set<String> subEquality = new TreeSet<>();
				final Set<String> subRange = new TreeSet<>();
				if (entry.getValue().isDocument()) {
					walk(entry.getValue().asDocument(), subEquality, subRange, containsOr);
				}
				if (subEquality.isEmpty() && subRange.isEmpty()) {
					equality.add(field);
				}
				subEquality.forEach(it -> equality.add(field + "." + it));
				subRange.forEach(it -> range.add(field + "." + it));
			} else {
				// $gt, $gte, $lt, $lte, $ne, $nin, $exists, $regex, $mod, $size, $type, $not...
				range.add(field);
			}
		}
	}

	/**
	 * Returns the stable textual key of this signature, used to aggregate the occurrences.
	 * @return the signature key, for example {@code user|FIND|E:active,companyId|S:createdAt:-1|R:age}
	 */
	public String getKey() {
		return this.key;
	}

	/**
	 * Returns the collection the query runs on.
	 * @return the collection name
	 */
	public String getCollection() {
		return this.collection;
	}

	/**
	 * Returns the kind of operation that carried the filter.
	 * @return the operation
	 */
	public Operation getOperation() {
		return this.operation;
	}

	/**
	 * Returns the fields matched by equality, sorted alphabetically.
	 * @return the equality fields
	 */
	public List<String> getEquality() {
		return this.equality;
	}

	/**
	 * Returns the sort fields, in order, each rendered as {@code field:1} or {@code field:-1}.
	 * @return the sort fields
	 */
	public List<String> getSort() {
		return this.sort;
	}

	/**
	 * Returns the fields matched by a range predicate, sorted alphabetically.
	 * @return the range fields
	 */
	public List<String> getRange() {
		return this.range;
	}

	/**
	 * Returns the soft-delete field implicitly excluded by the query.
	 * @return the soft-delete field name, or {@code null} when the query does not exclude soft-deleted documents
	 */
	public String getSoftDeleteField() {
		return this.softDeleteField;
	}

	/**
	 * Tells whether the filter contains a {@code $or}/{@code $nor}.
	 * @return {@code true} when a single compound index cannot serve the whole filter
	 */
	public boolean isContainsOr() {
		return this.containsOr;
	}

	/**
	 * Builds the compound index this query would ideally use, following the ESR order.
	 *
	 * <p>
	 * The soft-delete field is deliberately left out: it is filtered with
	 * {@code $or(eq(deleted,false), exists(deleted,false))}, which a plain compound index serves
	 * poorly. The right answer for it is a <i>partial</i> index
	 * ({@code partialFilterExpression: {deleted: false}}), which is a decision to take per
	 * collection, not something to infer from a counter.
	 * </p>
	 *
	 * <p>
	 * A field may legitimately show up in two ESR groups at once — {@code sort("age").gt("age", 18)}
	 * puts {@code age} in both S and R. An index key can only name a field once, so the first
	 * position wins: it is the one that constrains the index, and the later group is already served
	 * by it.
	 * </p>
	 *
	 * @return the suggested index, for example {@code {"companyId": 1, "active": 1, "createdAt": -1, "age": 1}},
	 *         or {@code null} when the query filters and sorts on nothing (a full collection scan
	 *         that no index can help)
	 */
	public String getSuggestedIndex() {
		final List<String> parts = new ArrayList<>();
		final Set<String> used = new HashSet<>();
		for (final String field : this.equality) {
			if (used.add(field)) {
				parts.add("\"" + field + "\": 1");
			}
		}
		for (final String field : this.sort) {
			final int separator = field.lastIndexOf(':');
			final String name = field.substring(0, separator);
			if (used.add(name)) {
				parts.add("\"" + name + "\": " + field.substring(separator + 1));
			}
		}
		for (final String field : this.range) {
			if (used.add(field)) {
				parts.add("\"" + field + "\": 1");
			}
		}
		if (parts.isEmpty()) {
			return null;
		}
		return "{" + String.join(", ", parts) + "}";
	}

	@Override
	public String toString() {
		return this.key;
	}
}
