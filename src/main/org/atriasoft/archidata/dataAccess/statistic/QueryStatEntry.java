package org.atriasoft.archidata.dataAccess.statistic;

import java.util.List;

/**
 * One line of the query-statistics JSON file: a query shape and how many times it has been run.
 *
 * <p>
 * Plain mutable bean with public fields on purpose: it is the on-disk format, read back at start-up
 * to resume the counters across restarts, so it must stay trivially serializable by Jackson without
 * any extra module.
 * </p>
 *
 * @see QueryStatistics
 */
public class QueryStatEntry {
	/** Stable key of the query shape, for example {@code user|FIND|E:active,companyId|S:createdAt:-1|R:age}. */
	public String signature;
	/** Collection the query runs on. */
	public String collection;
	/** Kind of operation, see {@link QuerySignature.Operation}. */
	public String operation;
	/** Fields matched by equality, sorted alphabetically. */
	public List<String> equality;
	/** Sort fields, in order, each rendered as {@code field:1} or {@code field:-1}. */
	public List<String> sort;
	/** Fields matched by a range predicate, sorted alphabetically. */
	public List<String> range;
	/** Soft-delete field implicitly excluded by the query, or {@code null}. */
	public String softDeleteField;
	/** {@code true} when the filter holds a {@code $or}/{@code $nor} that a single index cannot serve. */
	public boolean containsOr;
	/** Number of times this query shape has been executed, cumulated across restarts. */
	public long count;
	/** The compound index this query shape would ideally use, or {@code null} for a full scan. */
	public String suggestedIndex;

	/** Creates an empty entry. */
	public QueryStatEntry() {}
}
