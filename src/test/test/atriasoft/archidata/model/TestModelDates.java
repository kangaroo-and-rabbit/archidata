package test.atriasoft.archidata.model;

import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.atriasoft.archidata.journal.ChangeJournalEntry;
import org.atriasoft.archidata.journal.ChangeJournalMarker;
import org.atriasoft.archidata.journal.ChangeJournalReport;
import org.atriasoft.archidata.model.Data;
import org.atriasoft.archidata.model.GenericTiming;
import org.atriasoft.archidata.model.GenericToken;
import org.atriasoft.archidata.model.User;
import org.atriasoft.archidata.tools.DateTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** A {@link Date} is mutable: changing it in place must not change the object it was given to or read from. */
class TestModelDates {
	private static final long TIME = 1_000_000L;

	private static <T> void keepsItsDate(
			final T bean,
			final BiConsumer<T, Date> setter,
			final Function<T, Date> getter) {
		final Date given = new Date(TIME);
		setter.accept(bean, given);
		given.setTime(0L);
		Assertions.assertEquals(new Date(TIME), getter.apply(bean));
		getter.apply(bean).setTime(0L);
		Assertions.assertEquals(new Date(TIME), getter.apply(bean));
		setter.accept(bean, null);
		Assertions.assertNull(getter.apply(bean));
	}

	@Test
	void theBeansKeepTheirDates() {
		keepsItsDate(new GenericTiming(), GenericTiming::setCreatedAt, GenericTiming::getCreatedAt);
		keepsItsDate(new GenericTiming(), GenericTiming::setUpdatedAt, GenericTiming::getUpdatedAt);
		keepsItsDate(new Data(), Data::setStreamsAnalyzedAt, Data::getStreamsAnalyzedAt);
		keepsItsDate(new GenericToken(), GenericToken::setEndValidity, GenericToken::getEndValidity);
		keepsItsDate(new User(), User::setLastConnection, User::getLastConnection);
		keepsItsDate(new ChangeJournalEntry(), ChangeJournalEntry::setSourceUpdatedAt,
				ChangeJournalEntry::getSourceUpdatedAt);
		keepsItsDate(new ChangeJournalEntry(), ChangeJournalEntry::setRecordedAt, ChangeJournalEntry::getRecordedAt);
		keepsItsDate(new ChangeJournalMarker(), ChangeJournalMarker::setLastSavedAt,
				ChangeJournalMarker::getLastSavedAt);
		keepsItsDate(new ChangeJournalMarker(), ChangeJournalMarker::setLastRunAt, ChangeJournalMarker::getLastRunAt);
	}

	@Test
	void theReportKeepsItsDate() {
		final Date given = new Date(TIME);
		final ChangeJournalReport report = new ChangeJournalReport(given, Map.of(), List.of());
		given.setTime(0L);
		report.runDate().setTime(0L);
		Assertions.assertEquals(new Date(TIME), report.runDate());
	}

	@Test
	void theReportKeepsItsCounters() {
		final Map<String, Long> captured = new HashMap<>();
		captured.put("one", 1L);
		final ChangeJournalReport report = new ChangeJournalReport(new Date(TIME), captured, List.of());
		captured.put("two", 2L);
		Assertions.assertEquals(1L, report.totalCaptured());
	}

	@Test
	void aCopyKeepsTheClassOfTheDate() {
		final Timestamp timestamp = new Timestamp(TIME);
		timestamp.setNanos(123_456_789);
		final Date copy = DateTools.copyOf(timestamp);
		Assertions.assertNotSame(timestamp, copy);
		Assertions.assertEquals(timestamp, copy);
		Assertions.assertEquals(123_456_789, ((Timestamp) copy).getNanos());
	}
}
