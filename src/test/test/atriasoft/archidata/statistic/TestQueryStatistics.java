package test.atriasoft.archidata.statistic;

import java.nio.file.Files;
import java.nio.file.Path;

import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.statistic.QuerySignature.Operation;
import org.atriasoft.archidata.dataAccess.statistic.QueryStatEntry;
import org.atriasoft.archidata.dataAccess.statistic.QueryStatReport;
import org.atriasoft.archidata.dataAccess.statistic.QueryStatistics;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the accumulation and the JSON persistence of {@link QueryStatistics}.
 */
public class TestQueryStatistics {

	@TempDir
	Path tempDir;

	@AfterEach
	public void tearDown() {
		QueryStatistics.reset();
		ConfigBaseVariable.setQueryStatisticsFile(null);
	}

	/**
	 * Finds an entry by its signature key in a report.
	 * @param report the report to search
	 * @param signature the signature key to look for
	 * @return the matching entry, or {@code null}
	 */
	private static QueryStatEntry find(final QueryStatReport report, final String signature) {
		return report.queries.stream().filter(it -> signature.equals(it.signature)).findFirst().orElse(null);
	}

	@Test
	public void testDisabledByDefault() {
		Assertions.assertFalse(QueryStatistics.isEnabled(), "no QUERY_STATISTICS_FILE => mode off");
		QueryStatistics.record("user", Operation.FIND, Filters.eq("name", "bob"), null, null);
		Assertions.assertTrue(QueryStatistics.buildReport().queries.isEmpty(), "nothing must be recorded when off");
	}

	@Test
	public void testEnabledByTheEnvironmentVariable() {
		ConfigBaseVariable.setQueryStatisticsFile(this.tempDir.resolve("stats.json").toString());
		Assertions.assertTrue(QueryStatistics.isEnabled());
	}

	@Test
	public void testOccurrencesAreAccumulatedPerShape() {
		ConfigBaseVariable.setQueryStatisticsFile(this.tempDir.resolve("stats.json").toString());
		QueryStatistics.record("user", Operation.FIND, Filters.eq("name", "bob"), null, null);
		QueryStatistics.record("user", Operation.FIND, Filters.eq("name", "alice"), null, null);
		QueryStatistics.record("user", Operation.FIND, Filters.eq("login", "bob"), null, null);

		final QueryStatReport report = QueryStatistics.buildReport();
		Assertions.assertEquals(2, report.queries.size());
		Assertions.assertEquals(2, find(report, "user|FIND|E:name|S:|R:").count);
		Assertions.assertEquals(1, find(report, "user|FIND|E:login|S:|R:").count);
	}

	@Test
	public void testReportIsSortedByDecreasingCount() {
		ConfigBaseVariable.setQueryStatisticsFile(this.tempDir.resolve("stats.json").toString());
		QueryStatistics.record("user", Operation.FIND, Filters.eq("rare", 1), null, null);
		for (int iii = 0; iii < 5; iii++) {
			QueryStatistics.record("user", Operation.FIND, Filters.eq("hot", 1), null, null);
		}
		final QueryStatReport report = QueryStatistics.buildReport();
		Assertions.assertEquals("user|FIND|E:hot|S:|R:", report.queries.get(0).signature);
		Assertions.assertEquals(5, report.queries.get(0).count);
	}

	@Test
	public void testFlushWritesTheJsonFile() throws Exception {
		final Path file = this.tempDir.resolve("stats.json");
		ConfigBaseVariable.setQueryStatisticsFile(file.toString());
		QueryStatistics.record("user", Operation.FIND, Filters.eq("companyId", 7), null, "deleted");
		QueryStatistics.flush();

		Assertions.assertTrue(Files.isRegularFile(file));
		final String content = Files.readString(file);
		Assertions.assertTrue(content.contains("user|FIND|E:companyId|S:|R:"), content);
		Assertions.assertTrue(content.contains("\\\"companyId\\\": 1"), content);
		Assertions.assertTrue(content.contains("\"deleted\""), content);
	}

	@Test
	public void testFlushCreatesTheParentDirectory() {
		final Path file = this.tempDir.resolve("nested/deeper/stats.json");
		ConfigBaseVariable.setQueryStatisticsFile(file.toString());
		QueryStatistics.record("user", Operation.FIND, Filters.eq("name", "bob"), null, null);
		QueryStatistics.flush();
		Assertions.assertTrue(Files.isRegularFile(file));
	}

	@Test
	public void testCountersSurviveARestart() {
		final Path file = this.tempDir.resolve("stats.json");
		ConfigBaseVariable.setQueryStatisticsFile(file.toString());
		QueryStatistics.record("user", Operation.FIND, Filters.eq("name", "bob"), null, null);
		QueryStatistics.record("user", Operation.FIND, Filters.eq("name", "bob"), null, null);
		QueryStatistics.flush();

		// Simulate a restart: drop everything in memory, keep the file.
		QueryStatistics.reset();
		QueryStatistics.record("user", Operation.FIND, Filters.eq("name", "zoe"), null, null);

		final QueryStatReport report = QueryStatistics.buildReport();
		Assertions.assertEquals(3, find(report, "user|FIND|E:name|S:|R:").count,
				"the 2 occurrences of the previous run must be cumulated with the new one");
	}

	@Test
	public void testShapeNotMetAgainKeepsItsDescription() {
		final Path file = this.tempDir.resolve("stats.json");
		ConfigBaseVariable.setQueryStatisticsFile(file.toString());
		QueryStatistics.record("user", Operation.FIND, Filters.eq("companyId", 7), null, null);
		QueryStatistics.flush();

		// Restart, and only ever run a *different* query.
		QueryStatistics.reset();
		QueryStatistics.record("group", Operation.FIND, Filters.eq("name", "x"), null, null);

		final QueryStatEntry old = find(QueryStatistics.buildReport(), "user|FIND|E:companyId|S:|R:");
		Assertions.assertNotNull(old);
		Assertions.assertEquals(1, old.count);
		Assertions.assertEquals("{\"companyId\": 1}", old.suggestedIndex, "description must not be lost");
		Assertions.assertEquals("user", old.collection);
	}

	@Test
	public void testCorruptedFileIsIgnored() throws Exception {
		final Path file = this.tempDir.resolve("stats.json");
		Files.writeString(file, "this is not json {{{");
		ConfigBaseVariable.setQueryStatisticsFile(file.toString());

		QueryStatistics.record("user", Operation.FIND, Filters.eq("name", "bob"), null, null);
		Assertions.assertEquals(1, find(QueryStatistics.buildReport(), "user|FIND|E:name|S:|R:").count);
	}

	@Test
	public void testFlushIsANoOpWhenDisabled() {
		final Path file = this.tempDir.resolve("stats.json");
		QueryStatistics.flush();
		Assertions.assertFalse(Files.exists(file));
	}
}
