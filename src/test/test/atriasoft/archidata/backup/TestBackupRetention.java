package test.atriasoft.archidata.backup;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.atriasoft.archidata.backup.BackupEngine;
import org.atriasoft.archidata.backup.BackupEngine.EngineBackupType;
import org.atriasoft.archidata.backup.RetentionPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TestBackupRetention {

	private static final String BASE_NAME = "test_backup";
	private static final LocalDate REF_DATE = LocalDate.of(2025, 6, 20);

	private void createFakeBackup(final Path dir, final String sequence, final boolean partial) throws IOException {
		final String fileName = BASE_NAME + "_" + sequence + (partial ? "_partial" : "") + ".tar.gz";
		Files.createFile(dir.resolve(fileName));
	}

	private BackupEngine createEngine(final Path dir) {
		return new BackupEngine(dir, BASE_NAME, EngineBackupType.JSON_EXTENDED);
	}

	private Set<String> fileNames(final List<Path> paths) {
		return paths.stream().map(p -> p.getFileName().toString()).collect(Collectors.toSet());
	}

	@Test
	public void testRetentionKeepAllRecent(@TempDir final Path tempDir) throws IOException {
		// Files within 5 days should all be kept
		createFakeBackup(tempDir, "2025-06-20", false);
		createFakeBackup(tempDir, "2025-06-19", false);
		createFakeBackup(tempDir, "2025-06-18", false);
		createFakeBackup(tempDir, "2025-06-17", false);
		createFakeBackup(tempDir, "2025-06-16", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		Assertions.assertEquals(0, toDelete.size(), "All recent files should be kept");
	}

	@Test
	public void testRetentionDailyDedup(@TempDir final Path tempDir) throws IOException {
		// Multiple backups on the same day between 5 days and 3 months → keep 1 per day
		// These are 10 days old → in daily zone
		createFakeBackup(tempDir, "2025-06-10T08-00-00", false);
		createFakeBackup(tempDir, "2025-06-10T14-00-00", false);
		createFakeBackup(tempDir, "2025-06-10T20-00-00", false);
		// Another day, single backup
		createFakeBackup(tempDir, "2025-06-09", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		// 2 of the 3 June 10 backups should be deleted (keep the latest = T20-00-00)
		Assertions.assertEquals(2, toDelete.size(), "Should delete 2 duplicate daily backups");
		final Set<String> deletedNames = fileNames(toDelete);
		Assertions.assertTrue(deletedNames.contains("test_backup_2025-06-10T08-00-00.tar.gz"));
		Assertions.assertTrue(deletedNames.contains("test_backup_2025-06-10T14-00-00.tar.gz"));
		Assertions.assertFalse(deletedNames.contains("test_backup_2025-06-10T20-00-00.tar.gz"));
	}

	@Test
	public void testRetentionWeeklyDedup(@TempDir final Path tempDir) throws IOException {
		// Files between 3 and 12 months → keep 1 per week
		// These are ~4 months old → in weekly zone
		createFakeBackup(tempDir, "2025-02-10", false);
		createFakeBackup(tempDir, "2025-02-11", false);
		createFakeBackup(tempDir, "2025-02-12", false);
		createFakeBackup(tempDir, "2025-02-13", false);
		// Different week
		createFakeBackup(tempDir, "2025-02-03", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		// Feb 10-13 are same week → keep only the latest (Feb 13)
		// Feb 3 is a different week → keep
		Assertions.assertEquals(3, toDelete.size(), "Should delete 3 weekly duplicates");
		final Set<String> deletedNames = fileNames(toDelete);
		Assertions.assertFalse(deletedNames.contains("test_backup_2025-02-13.tar.gz"), "Latest of week should be kept");
		Assertions.assertFalse(deletedNames.contains("test_backup_2025-02-03.tar.gz"), "Different week should be kept");
	}

	@Test
	public void testRetentionDeleteOld(@TempDir final Path tempDir) throws IOException {
		// Files older than 12 months → all deleted
		createFakeBackup(tempDir, "2024-01-15", false);
		createFakeBackup(tempDir, "2024-02-20", false);
		createFakeBackup(tempDir, "2024-03-10", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		Assertions.assertEquals(3, toDelete.size(), "All old files should be deleted");
	}

	@Test
	public void testRetentionMixedZones(@TempDir final Path tempDir) throws IOException {
		// Zone 1: recent (keep all)
		createFakeBackup(tempDir, "2025-06-20", false);
		createFakeBackup(tempDir, "2025-06-19", false);
		// Zone 2: daily (keep 1/day) - 2 backups same day
		createFakeBackup(tempDir, "2025-06-01T08-00-00", false);
		createFakeBackup(tempDir, "2025-06-01T18-00-00", false);
		// Zone 3: weekly (keep 1/week) - 2 backups same week
		createFakeBackup(tempDir, "2025-01-06", false); // Monday
		createFakeBackup(tempDir, "2025-01-07", false); // Tuesday (same week)
		// Zone 4: too old (delete)
		createFakeBackup(tempDir, "2024-01-01", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		// 1 daily dup + 1 weekly dup + 1 too old = 3 deleted
		Assertions.assertEquals(3, toDelete.size(), "Should delete 1 daily dup + 1 weekly dup + 1 old");
		final Set<String> deletedNames = fileNames(toDelete);
		Assertions.assertTrue(deletedNames.contains("test_backup_2025-06-01T08-00-00.tar.gz"));
		Assertions.assertTrue(deletedNames.contains("test_backup_2025-01-06.tar.gz"));
		Assertions.assertTrue(deletedNames.contains("test_backup_2024-01-01.tar.gz"));
	}

	@Test
	public void testRetentionPartialFiles(@TempDir final Path tempDir) throws IOException {
		// Partial backup files should also be handled
		createFakeBackup(tempDir, "2024-01-15", true);
		createFakeBackup(tempDir, "2025-06-19", true);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		// Old partial should be marked for deletion, recent should be kept
		Assertions.assertEquals(1, toDelete.size());
		Assertions.assertTrue(toDelete.get(0).getFileName().toString().contains("2024-01-15"));
		// Both files should still exist (dry run)
		Assertions.assertTrue(Files.exists(tempDir.resolve("test_backup_2024-01-15_partial.tar.gz")));
		Assertions.assertTrue(Files.exists(tempDir.resolve("test_backup_2025-06-19_partial.tar.gz")));
	}

	@Test
	public void testRetentionDryRun(@TempDir final Path tempDir) throws IOException {
		// Dry run should not delete files
		createFakeBackup(tempDir, "2024-01-15", false);
		createFakeBackup(tempDir, "2024-02-20", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> wouldDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		Assertions.assertEquals(2, wouldDelete.size());
		// Files should still exist
		for (final Path path : wouldDelete) {
			Assertions.assertTrue(Files.exists(path), "Dry run should not delete files: " + path);
		}
	}

	@Test
	public void testRetentionCleanActuallyDeletes(@TempDir final Path tempDir) throws IOException {
		// Clean should actually delete files
		createFakeBackup(tempDir, "2024-01-15", false);
		createFakeBackup(tempDir, "2025-06-19", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> deleted = engine.clean(new RetentionPolicy(5, 3, 12), REF_DATE);

		Assertions.assertEquals(1, deleted.size());
		Assertions.assertFalse(Files.exists(deleted.get(0)), "Clean should have deleted the file");
		// Recent file should still exist
		Assertions.assertTrue(Files.exists(tempDir.resolve("test_backup_2025-06-19.tar.gz")));
	}

	@Test
	public void testRetentionNoFiles(@TempDir final Path tempDir) throws IOException {
		// Empty directory should not cause errors
		final BackupEngine engine = createEngine(tempDir);
		final List<Path> deleted = engine.clean(new RetentionPolicy(5, 3, 12), REF_DATE);

		Assertions.assertEquals(0, deleted.size());
	}

	@Test
	public void testRetentionUnparsableDate(@TempDir final Path tempDir) throws IOException {
		// Files with non-date sequences should be ignored (not deleted)
		createFakeBackup(tempDir, "manual", false);
		createFakeBackup(tempDir, "v1.0", false);
		createFakeBackup(tempDir, "2025-06-19", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> deleted = engine.clean(new RetentionPolicy(5, 3, 12), REF_DATE);

		// Only parseable old files would be deleted; 2025-06-19 is recent → kept
		Assertions.assertEquals(0, deleted.size());
		// Non-parseable files should still exist
		Assertions.assertTrue(Files.exists(tempDir.resolve("test_backup_manual.tar.gz")));
		Assertions.assertTrue(Files.exists(tempDir.resolve("test_backup_v1.0.tar.gz")));
		Assertions.assertTrue(Files.exists(tempDir.resolve("test_backup_2025-06-19.tar.gz")));
	}

	@Test
	public void testRetentionPolicyValidation() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> new RetentionPolicy(-1, 3, 12));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new RetentionPolicy(5, -1, 12));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new RetentionPolicy(5, 3, -1));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new RetentionPolicy(5, 12, 3));
		// Valid
		Assertions.assertDoesNotThrow(() -> new RetentionPolicy(0, 0, 0));
		Assertions.assertDoesNotThrow(() -> new RetentionPolicy(5, 3, 12));
		Assertions.assertDoesNotThrow(() -> new RetentionPolicy(5, 3, 3));
	}

	@Test
	public void testParseDateFromSequence() {
		Assertions.assertEquals(LocalDate.of(2025, 6, 20), BackupEngine.parseDateFromSequence("2025-06-20"));
		Assertions.assertEquals(LocalDate.of(2025, 6, 20), BackupEngine.parseDateFromSequence("2025-06-20T14-30-00"));
		Assertions.assertEquals(LocalDate.of(2025, 6, 20), BackupEngine.parseDateFromSequence("2025-06-20_14-30-00"));
		Assertions.assertNull(BackupEngine.parseDateFromSequence("manual"));
		Assertions.assertNull(BackupEngine.parseDateFromSequence("v1.0"));
		Assertions.assertNull(BackupEngine.parseDateFromSequence(""));
		Assertions.assertNull(BackupEngine.parseDateFromSequence(null));
	}

	// ===== Edge case tests =====

	@Test
	public void testRetentionBoundaryExactlyAtKeepAllLimit(@TempDir final Path tempDir) throws IOException {
		// File exactly at the keepAllDays boundary (5 days old) should be in the daily zone, NOT kept-all
		// REF_DATE = 2025-06-20, keepAllDays=5 → keepAllLimit = 2025-06-15
		// File on 2025-06-15 has date == keepAllLimit → NOT in keep-all zone (date < keepAllLimit is false,
		// but date >= keepAllLimit check uses !isBefore which means >=, so 06-15 IS in keep-all zone)
		createFakeBackup(tempDir, "2025-06-15", false);
		// File on 2025-06-14 is 6 days old → in daily zone
		createFakeBackup(tempDir, "2025-06-14", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		// Both should be kept (06-15 in keep-all, 06-14 alone in its day in daily zone)
		Assertions.assertEquals(0, toDelete.size(), "Boundary files should be kept");
	}

	@Test
	public void testRetentionBoundaryExactlyAtDailyLimit(@TempDir final Path tempDir) throws IOException {
		// File exactly at keepDailyMonths boundary (3 months)
		// REF_DATE = 2025-06-20, keepDailyMonths=3 → keepDailyLimit = 2025-03-20
		// File on 2025-03-20 should be in daily zone (>= keepDailyLimit)
		createFakeBackup(tempDir, "2025-03-20", false);
		// File on 2025-03-19 should be in weekly zone (< keepDailyLimit)
		createFakeBackup(tempDir, "2025-03-19", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		// Both alone in their group → no duplicates, nothing deleted
		Assertions.assertEquals(0, toDelete.size(), "Boundary files alone in their groups should be kept");
	}

	@Test
	public void testRetentionBoundaryExactlyAtWeeklyLimit(@TempDir final Path tempDir) throws IOException {
		// File exactly at keepWeeklyMonths boundary (12 months)
		// REF_DATE = 2025-06-20, keepWeeklyMonths=12 → keepWeeklyLimit = 2024-06-20
		// File on 2024-06-20 should be in weekly zone (>= keepWeeklyLimit)
		createFakeBackup(tempDir, "2024-06-20", false);
		// File on 2024-06-19 should be deleted (< keepWeeklyLimit)
		createFakeBackup(tempDir, "2024-06-19", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		Assertions.assertEquals(1, toDelete.size(), "Only file beyond weekly limit should be deleted");
		final Set<String> deletedNames = fileNames(toDelete);
		Assertions.assertTrue(deletedNames.contains("test_backup_2024-06-19.tar.gz"));
		Assertions.assertFalse(deletedNames.contains("test_backup_2024-06-20.tar.gz"));
	}

	@Test
	public void testRetentionZeroKeepAllDays(@TempDir final Path tempDir) throws IOException {
		// keepAllDays=0 → keepAllLimit = REF_DATE (2025-06-20)
		// Files on REF_DATE have date >= keepAllLimit → still in keep-all zone
		// So we need files BEFORE REF_DATE to test daily dedup
		createFakeBackup(tempDir, "2025-06-19T08-00-00", false);
		createFakeBackup(tempDir, "2025-06-19T12-00-00", false);
		createFakeBackup(tempDir, "2025-06-19T18-00-00", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(0, 3, 12), REF_DATE);

		// All 3 are same day in daily zone → keep only latest (T18-00-00)
		Assertions.assertEquals(2, toDelete.size());
		final Set<String> deletedNames = fileNames(toDelete);
		Assertions.assertFalse(deletedNames.contains("test_backup_2025-06-19T18-00-00.tar.gz"));
	}

	@Test
	public void testRetentionZeroAllValues(@TempDir final Path tempDir) throws IOException {
		// keepAllDays=0, keepDailyMonths=0, keepWeeklyMonths=0
		// keepAllLimit = REF_DATE, keepDailyLimit = REF_DATE, keepWeeklyLimit = REF_DATE
		// File on REF_DATE: date >= keepAllLimit → keep-all zone (kept)
		// File before REF_DATE: date < keepAllLimit AND date < keepDailyLimit AND date < keepWeeklyLimit → deleted
		createFakeBackup(tempDir, "2025-06-20", false);
		createFakeBackup(tempDir, "2025-06-19", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(0, 0, 0), REF_DATE);

		// Only 2025-06-19 is deleted, 2025-06-20 (= REF_DATE) stays in keep-all
		Assertions.assertEquals(1, toDelete.size(), "Only files before reference date should be deleted");
		Assertions.assertTrue(toDelete.get(0).getFileName().toString().contains("2025-06-19"));
	}

	@Test
	public void testRetentionSameDayDifferentFormats(@TempDir final Path tempDir) throws IOException {
		// Same day expressed in different sequence formats → should dedup to 1
		// In daily zone (10 days old)
		createFakeBackup(tempDir, "2025-06-10", false);
		createFakeBackup(tempDir, "2025-06-10T09-00-00", false);
		createFakeBackup(tempDir, "2025-06-10_15-30-00", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		// All parse to same day, keep the latest by sequence comparison
		// "2025-06-10_15-30-00" > "2025-06-10T09-00-00" > "2025-06-10" (lexicographic)
		Assertions.assertEquals(2, toDelete.size(), "Should dedup same-day different formats");
		final Set<String> deletedNames = fileNames(toDelete);
		Assertions.assertFalse(deletedNames.contains("test_backup_2025-06-10_15-30-00.tar.gz"),
				"Latest sequence should be kept");
	}

	@Test
	public void testRetentionMixedPartialAndFull(@TempDir final Path tempDir) throws IOException {
		// Partial and full backups on the same day in daily zone
		createFakeBackup(tempDir, "2025-06-10T08-00-00", false);
		createFakeBackup(tempDir, "2025-06-10T12-00-00", true); // partial
		createFakeBackup(tempDir, "2025-06-10T18-00-00", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		// All same day → keep latest (T18-00-00), delete others including partial
		Assertions.assertEquals(2, toDelete.size());
		final Set<String> deletedNames = fileNames(toDelete);
		Assertions.assertTrue(deletedNames.contains("test_backup_2025-06-10T08-00-00.tar.gz"));
		Assertions.assertTrue(deletedNames.contains("test_backup_2025-06-10T12-00-00_partial.tar.gz"));
		Assertions.assertFalse(deletedNames.contains("test_backup_2025-06-10T18-00-00.tar.gz"));
	}

	@Test
	public void testRetentionWeeklySpanningYearBoundary(@TempDir final Path tempDir) throws IOException {
		// Backups at year boundary in the weekly zone
		// REF_DATE = 2025-06-20, keepDailyMonths=3, keepWeeklyMonths=12
		// Files from late December 2024 / early January 2025 (~6 months old, in weekly zone)
		// Dec 30 2024 and Dec 31 2024 are in ISO week 1 of 2025
		// Jan 1, 2, 3 2025 are also in ISO week 1 of 2025
		createFakeBackup(tempDir, "2024-12-30", false);
		createFakeBackup(tempDir, "2024-12-31", false);
		createFakeBackup(tempDir, "2025-01-01", false);
		createFakeBackup(tempDir, "2025-01-02", false);
		createFakeBackup(tempDir, "2025-01-03", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		// All in same ISO week → keep only the latest (Jan 3)
		Assertions.assertEquals(4, toDelete.size(), "Should keep only 1 from the year-boundary week");
		final Set<String> deletedNames = fileNames(toDelete);
		Assertions.assertFalse(deletedNames.contains("test_backup_2025-01-03.tar.gz"),
				"Latest of year-boundary week should be kept");
	}

	@Test
	public void testRetentionLargeKeepAllDays(@TempDir final Path tempDir) throws IOException {
		// keepAllDays=365 effectively keeps everything from the last year
		createFakeBackup(tempDir, "2025-06-20", false);
		createFakeBackup(tempDir, "2025-01-01", false);
		createFakeBackup(tempDir, "2024-07-01", false);
		// This one is older than 365 days → in daily or weekly zone
		createFakeBackup(tempDir, "2024-06-01", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(365, 0, 0), REF_DATE);

		// Everything within 365 days is kept, but keepDailyMonths=0 and keepWeeklyMonths=0
		// means anything outside keep-all zone is deleted
		// 2024-06-01 is 384 days old → deleted
		// 2024-07-01 is 354 days old → kept (within 365 days)
		Assertions.assertEquals(1, toDelete.size());
		final Set<String> deletedNames = fileNames(toDelete);
		Assertions.assertTrue(deletedNames.contains("test_backup_2024-06-01.tar.gz"));
	}

	@Test
	public void testRetentionOnlyWeeklyZone(@TempDir final Path tempDir) throws IOException {
		// keepAllDays=0, keepDailyMonths=0, keepWeeklyMonths=12
		// keepAllLimit = REF_DATE, keepDailyLimit = REF_DATE, keepWeeklyLimit = REF_DATE - 12 months
		// File on REF_DATE (2025-06-20) → date >= keepAllLimit → keep-all zone
		// Files before REF_DATE → date < keepAllLimit AND date < keepDailyLimit → weekly zone
		createFakeBackup(tempDir, "2025-06-17", false); // Tuesday
		createFakeBackup(tempDir, "2025-06-18", false); // Wednesday (same week)
		createFakeBackup(tempDir, "2025-06-19", false); // Thursday (same week)
		createFakeBackup(tempDir, "2025-06-10", false); // different week

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(0, 0, 12), REF_DATE);

		// 06-17, 06-18, 06-19 are same week → keep latest (06-19)
		// 06-10 is different week → keep
		Assertions.assertEquals(2, toDelete.size());
		final Set<String> deletedNames = fileNames(toDelete);
		Assertions.assertFalse(deletedNames.contains("test_backup_2025-06-19.tar.gz"));
		Assertions.assertFalse(deletedNames.contains("test_backup_2025-06-10.tar.gz"));
	}

	@Test
	public void testRetentionForeignFilesIgnored(@TempDir final Path tempDir) throws IOException {
		// Files that don't match the baseName pattern should not be touched
		Files.createFile(tempDir.resolve("other_backup_2024-01-01.tar.gz"));
		Files.createFile(tempDir.resolve("random_file.txt"));
		Files.createFile(tempDir.resolve("test_backup_without_extension"));
		createFakeBackup(tempDir, "2025-06-19", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		Assertions.assertEquals(0, toDelete.size(), "Foreign files should not be affected");
		// Verify all foreign files still exist
		Assertions.assertTrue(Files.exists(tempDir.resolve("other_backup_2024-01-01.tar.gz")));
		Assertions.assertTrue(Files.exists(tempDir.resolve("random_file.txt")));
		Assertions.assertTrue(Files.exists(tempDir.resolve("test_backup_without_extension")));
	}

	@Test
	public void testRetentionDefaultPolicy(@TempDir final Path tempDir) throws IOException {
		// Test the default RetentionPolicy constructor (7, 3, 24)
		final RetentionPolicy defaultPolicy = new RetentionPolicy();
		Assertions.assertEquals(7, defaultPolicy.keepAllDays());
		Assertions.assertEquals(3, defaultPolicy.keepDailyMonths());
		Assertions.assertEquals(24, defaultPolicy.keepWeeklyMonths());

		// Recent file should be kept with default policy
		createFakeBackup(tempDir, "2025-06-20", false);
		// 2 years old file should be kept (within 24 months)
		createFakeBackup(tempDir, "2023-07-01", false);
		// 3 years old file should be deleted (beyond 24 months)
		createFakeBackup(tempDir, "2022-06-01", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(defaultPolicy, REF_DATE);

		Assertions.assertEquals(1, toDelete.size());
		Assertions.assertTrue(toDelete.get(0).getFileName().toString().contains("2022-06-01"));
	}

	@Test
	public void testRetentionCleanMultipleDeletes(@TempDir final Path tempDir) throws IOException {
		// Clean (not dry run) should delete multiple files
		createFakeBackup(tempDir, "2024-01-15", false);
		createFakeBackup(tempDir, "2024-02-20", false);
		createFakeBackup(tempDir, "2024-03-10", false);
		createFakeBackup(tempDir, "2025-06-19", false); // recent → kept

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> deleted = engine.clean(new RetentionPolicy(5, 3, 12), REF_DATE);

		Assertions.assertEquals(3, deleted.size());
		for (final Path path : deleted) {
			Assertions.assertFalse(Files.exists(path), "Clean should have deleted: " + path);
		}
		Assertions.assertTrue(Files.exists(tempDir.resolve("test_backup_2025-06-19.tar.gz")),
				"Recent file should still exist");
	}

	@Test
	public void testCleanWithoutReferenceDateUsesToday(@TempDir final Path tempDir) throws IOException {
		// clean(policy) without referenceDate should use LocalDate.now()
		// Create a file that is certainly old enough to be deleted (5 years ago)
		createFakeBackup(tempDir, "2020-01-01", false);
		// Create a file for today that should be kept
		createFakeBackup(tempDir, LocalDate.now().toString(), false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> deleted = engine.clean(new RetentionPolicy(5, 3, 12));

		// The old file should be deleted, the recent one kept
		Assertions.assertEquals(1, deleted.size());
		Assertions.assertTrue(deleted.get(0).getFileName().toString().contains("2020-01-01"));
		Assertions.assertFalse(Files.exists(deleted.get(0)), "Clean should have deleted the file");
	}

	@Test
	public void testCleanDryRunWithoutReferenceDateUsesToday(@TempDir final Path tempDir) throws IOException {
		// cleanDryRun(policy) without referenceDate should use LocalDate.now()
		createFakeBackup(tempDir, "2020-01-01", false);
		createFakeBackup(tempDir, LocalDate.now().toString(), false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> wouldDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12));

		Assertions.assertEquals(1, wouldDelete.size());
		Assertions.assertTrue(wouldDelete.get(0).getFileName().toString().contains("2020-01-01"));
		// Dry run — file should still exist
		Assertions.assertTrue(Files.exists(wouldDelete.get(0)));
	}

	@Test
	public void testListBackupFilesSkipsHiddenFiles(@TempDir final Path tempDir) throws IOException {
		// Hidden files (starting with '.') should be ignored by listBackupFiles
		createFakeBackup(tempDir, "2025-06-19", false);
		// Create a hidden temp file that matches the prefix pattern
		Files.createFile(tempDir.resolve("." + BASE_NAME + "_2025-06-18.tar.gz_tmp"));

		final BackupEngine engine = createEngine(tempDir);
		// cleanDryRun uses listBackupFiles internally
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		// Only the visible file should be found and kept (it is recent)
		Assertions.assertEquals(0, toDelete.size());
		// Verify hidden file is untouched
		Assertions.assertTrue(Files.exists(tempDir.resolve("." + BASE_NAME + "_2025-06-18.tar.gz_tmp")));
	}

	@Test
	public void testRetentionSingleFilePerZone(@TempDir final Path tempDir) throws IOException {
		// One file in each zone, no duplicates → nothing deleted
		// Zone 1: recent
		createFakeBackup(tempDir, "2025-06-18", false);
		// Zone 2: daily (1 month old, single in its day)
		createFakeBackup(tempDir, "2025-05-15", false);
		// Zone 3: weekly (6 months old, single in its week)
		createFakeBackup(tempDir, "2024-12-15", false);

		final BackupEngine engine = createEngine(tempDir);
		final List<Path> toDelete = engine.cleanDryRun(new RetentionPolicy(5, 3, 12), REF_DATE);

		Assertions.assertEquals(0, toDelete.size(), "Single files per zone should all be kept");
	}
}
