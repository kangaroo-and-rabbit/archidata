package test.atriasoft.archidata.backup;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.atriasoft.archidata.backup.BackupEngine;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests of the date parsing of backup sequences (pure functions, no database).
 */
public class TestBackupSequenceParsing {

	@Test
	public void singleSuffixIsStripped() {
		Assertions.assertEquals(LocalDate.of(2026, 10, 15), BackupEngine.parseDateFromSequence("2026-10-15_full"));
		Assertions.assertEquals(LocalDate.of(2026, 10, 15), BackupEngine.parseDateFromSequence("2026-10-15_delta"));
		Assertions.assertEquals(LocalDate.of(2026, 10, 15), BackupEngine.parseDateFromSequence("2026-10-15"));
	}

	@Test
	public void fullDeltaSequenceIsParsable() {
		Assertions.assertEquals(LocalDate.of(2026, 10, 15),
				BackupEngine.parseDateFromSequence("2026-10-15_full_delta"));
		Assertions.assertEquals(LocalDateTime.of(2026, 10, 15, 3, 0, 0),
				BackupEngine.parseDateTimeFromSequence("2026-10-15_03:00:00.000_full_delta"));
		Assertions.assertEquals(LocalDateTime.of(2026, 10, 15, 3, 0, 0),
				BackupEngine.parseDateTimeFromSequence("2026-10-15_03-00-00_full_partial_delta"));
	}

	@Test
	public void timeWithUnderscoreIsKept() {
		Assertions.assertEquals(LocalDateTime.of(2026, 10, 15, 3, 15, 0),
				BackupEngine.parseDateTimeFromSequence("2026-10-15_03-15-00"));
		Assertions.assertEquals(LocalDateTime.of(2026, 10, 15, 3, 15, 0),
				BackupEngine.parseDateTimeFromSequence("2026-10-15_03-15-00_delta"));
	}

	@Test
	public void unparseableSequenceReturnsNull() {
		Assertions.assertNull(BackupEngine.parseDateFromSequence("full_delta"));
		Assertions.assertNull(BackupEngine.parseDateFromSequence("nightly"));
		Assertions.assertNull(BackupEngine.parseDateTimeFromSequence(""));
	}
}
