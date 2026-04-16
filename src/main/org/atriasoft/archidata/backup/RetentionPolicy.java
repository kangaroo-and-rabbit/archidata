package org.atriasoft.archidata.backup;

/**
 * Defines a backup retention policy with three configurable time zones:
 * <ul>
 *   <li><b>keepAllDays</b>: Keep ALL backups from the last N days</li>
 *   <li><b>keepDailyMonths</b>: Keep 1 backup per day for the last N months</li>
 *   <li><b>keepWeeklyMonths</b>: Keep 1 backup per week for the last N months</li>
 * </ul>
 * Backups older than keepWeeklyMonths are deleted.
 *
 * @param keepAllDays number of days to keep all backups (must be non-negative)
 * @param keepDailyMonths number of months to keep one daily backup (must be non-negative)
 * @param keepWeeklyMonths number of months to keep one weekly backup (must be non-negative and &gt;= keepDailyMonths)
 */
public record RetentionPolicy(
		int keepAllDays,
		int keepDailyMonths,
		int keepWeeklyMonths) {

	/** Default policy: 7 days all, 3 months daily, 24 months weekly. */
	public RetentionPolicy() {
		this(7, 3, 24);
	}

	/**
	 * Validates the retention policy parameters.
	 *
	 * @throws IllegalArgumentException if any parameter is negative or if keepWeeklyMonths is less than keepDailyMonths
	 */
	public RetentionPolicy {
		if (keepAllDays < 0) {
			throw new IllegalArgumentException("keepAllDays must be >= 0, got: " + keepAllDays);
		}
		if (keepDailyMonths < 0) {
			throw new IllegalArgumentException("keepDailyMonths must be >= 0, got: " + keepDailyMonths);
		}
		if (keepWeeklyMonths < 0) {
			throw new IllegalArgumentException("keepWeeklyMonths must be >= 0, got: " + keepWeeklyMonths);
		}
		if (keepWeeklyMonths < keepDailyMonths) {
			throw new IllegalArgumentException(
					"keepWeeklyMonths (" + keepWeeklyMonths + ") must be >= keepDailyMonths (" + keepDailyMonths + ")");
		}
	}
}
