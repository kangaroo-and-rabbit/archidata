/**
 * Query-shape statistics, to decide which MongoDB indexes are worth creating.
 *
 * <p>When enabled, every filtered query executed by the data access layer is reduced to a
 * {@link org.atriasoft.archidata.dataAccess.statistic.QuerySignature} (field names without their
 * values, ordered by the Equality-Sort-Range rule) and counted. The counters are flushed
 * periodically to a JSON file, whose content is described by
 * {@link org.atriasoft.archidata.dataAccess.statistic.QueryStatReport} and
 * {@link org.atriasoft.archidata.dataAccess.statistic.QueryStatEntry}; each entry carries a
 * {@code suggestedIndex}.</p>
 *
 * <p>Entry point: {@link org.atriasoft.archidata.dataAccess.statistic.QueryStatistics}. The mode is
 * off unless the {@code QUERY_STATISTICS_FILE} environment variable points to a writable path (the
 * flush period is {@code QUERY_STATISTICS_PERIOD}), so the calls are cheap to leave in production.
 * The data access layer calls {@code record} itself; an application only needs to read the file, or
 * to build the report on demand:</p>
 * {@snippet lang = java :
 * if (QueryStatistics.isEnabled()) {
 * 	QueryStatistics.flush();
 * 	final QueryStatReport report = QueryStatistics.buildReport();
 * 	for (final QueryStatEntry entry : report.queries) {
 * 		LOGGER.info("{} x{} -> {}", entry.signature, entry.count, entry.suggestedIndex);
 * 	}
 * }
 * }
 *
 * <p>See also the declarative index management in {@link org.atriasoft.archidata.index} and
 * <code>doc/indexes.md</code> in the repository
 * (<a href="https://github.com/kangaroo-and-rabbit/archidata/blob/develop/doc/indexes.md">online</a>).</p>
 */
package org.atriasoft.archidata.dataAccess.statistic;
