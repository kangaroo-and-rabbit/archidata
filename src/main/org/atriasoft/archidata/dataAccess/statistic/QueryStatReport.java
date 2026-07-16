package org.atriasoft.archidata.dataAccess.statistic;

import java.util.ArrayList;
import java.util.List;

/**
 * Root of the query-statistics JSON file.
 *
 * @see QueryStatistics
 */
public class QueryStatReport {
	/** ISO-8601 instant of the last flush. */
	public String generatedAt;
	/** Every known query shape, sorted by decreasing number of occurrences. */
	public List<QueryStatEntry> queries = new ArrayList<>();

	/** Creates an empty report. */
	public QueryStatReport() {}
}
