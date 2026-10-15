package org.atriasoft.archidata.cron;

import java.time.LocalDateTime;

/**
 * A cron expression parsed once: the allowed values of each field and the day restriction flags.
 *
 * <p>The syntax follows Unix (Vixie) cron: five fields {@code minute hour dayOfMonth month dayOfWeek}
 * separated by whitespace; each field is a comma-separated list of {@code *}, {@code n}, {@code a-b},
 * {@code *}{@code /s} or {@code a-b/s}. A step starts at the low bound of the field or of the range. The day of
 * week is 0-7, Sunday being 0 or 7. When both the day of month and the day of week are restricted (neither
 * starts with {@code *}), a day matches if either of them matches.
 */
final class CronExpression {
	/** Index of the minute field. */
	static final int MINUTE = 0;
	/** Index of the hour field. */
	static final int HOUR = 1;
	/** Index of the day-of-month field. */
	static final int DAY_OF_MONTH = 2;
	/** Index of the month field. */
	static final int MONTH = 3;
	/** Index of the day-of-week field. */
	static final int DAY_OF_WEEK = 4;
	/** Names of the five fields, for error messages. */
	static final String[] FIELD_NAMES = { "minute", "hour", "day of month", "month", "day of week" };
	/** Bounds of the five fields (day of week: 0 and 7 are both Sunday). */
	static final int[][] LIMITS = { { 0, 59 }, { 0, 23 }, { 1, 31 }, { 1, 12 }, { 0, 7 } };

	private final String expression;
	private final String[] parts;
	private final boolean[][] fields;
	private final boolean dayOfMonthRestricted;
	private final boolean dayOfWeekRestricted;

	private CronExpression(final String expression, final String[] parts, final boolean[][] fields) {
		this.expression = expression;
		this.parts = parts;
		this.fields = fields;
		this.dayOfMonthRestricted = isRestricted(parts[DAY_OF_MONTH]);
		this.dayOfWeekRestricted = isRestricted(parts[DAY_OF_WEEK]);
	}

	/**
	 * Parses and validates a cron expression.
	 *
	 * @param cronExpression the five-field expression
	 * @return the parsed expression
	 * @throws IllegalArgumentException if the expression is invalid, with the offending field
	 */
	static CronExpression parse(final String cronExpression) {
		final String[] parts = splitFields(cronExpression);
		return new CronExpression(cronExpression, parts, parseFields(parts));
	}

	/** @return the expression as written by the caller */
	String expression() {
		return this.expression;
	}

	/**
	 * @param index the field index ({@link #MINUTE} .. {@link #DAY_OF_WEEK})
	 * @return the field as written by the caller
	 */
	String part(final int index) {
		return this.parts[index];
	}

	/**
	 * @param index the field index ({@link #MINUTE} .. {@link #DAY_OF_WEEK})
	 * @param value the value to test (day of week: 0 or 7 is Sunday)
	 * @return {@code true} if the field allows this value
	 */
	boolean allows(final int index, final int value) {
		return this.fields[index][value];
	}

	/** @return {@code true} when both day fields are restricted, so that they combine with OR */
	boolean daysCombineWithOr() {
		return this.dayOfMonthRestricted && this.dayOfWeekRestricted;
	}

	/**
	 * Checks if the expression matches the given time (minute precision).
	 *
	 * @param time the time to test
	 * @return {@code true} if the expression is due at this minute
	 */
	boolean matches(final LocalDateTime time) {
		if (!this.fields[MINUTE][time.getMinute()] || !this.fields[HOUR][time.getHour()]
				|| !this.fields[MONTH][time.getMonthValue()]) {
			return false;
		}
		final boolean dayOfMonth = this.fields[DAY_OF_MONTH][time.getDayOfMonth()];
		// java.time: Monday = 1 .. Sunday = 7; cron: Sunday = 0 (7 is folded into 0 by parseFields)
		final boolean dayOfWeek = this.fields[DAY_OF_WEEK][time.getDayOfWeek().getValue() % 7];
		if (daysCombineWithOr()) {
			return dayOfMonth || dayOfWeek;
		}
		return dayOfMonth && dayOfWeek;
	}

	/** Vixie rule: a day field starting with '*' (including '*' + '/n') does not restrict the day. */
	private static boolean isRestricted(final String field) {
		return !field.startsWith("*");
	}

	private static String[] splitFields(final String cronExpression) {
		if (cronExpression == null) {
			throw new IllegalArgumentException("Cron expression must not be null");
		}
		final String[] parts = cronExpression.trim().split("\\s+");
		if (parts.length != 5) {
			throw new IllegalArgumentException(
					"Cron expression must have 5 fields (minute hour day month dayOfWeek): '" + cronExpression + "'");
		}
		return parts;
	}

	/** Expands the five fields into arrays of allowed values, indexed by value. */
	private static boolean[][] parseFields(final String[] parts) {
		final boolean[][] fields = new boolean[5][];
		for (int i = 0; i < parts.length; i++) {
			fields[i] = parseField(parts[i], LIMITS[i][0], LIMITS[i][1], FIELD_NAMES[i]);
		}
		// Sunday may be written 0 or 7
		if (fields[DAY_OF_WEEK][7]) {
			fields[DAY_OF_WEEK][0] = true;
		}
		if (fields[DAY_OF_WEEK][0]) {
			fields[DAY_OF_WEEK][7] = true;
		}
		return fields;
	}

	private static boolean[] parseField(final String field, final int min, final int max, final String fieldName) {
		final boolean[] allowed = new boolean[max + 1];
		for (final String part : field.split(",", -1)) {
			final String rangePart;
			int step = 1;
			final int slash = part.indexOf('/');
			if (slash >= 0) {
				rangePart = part.substring(0, slash);
				step = parseNumber(part.substring(slash + 1), fieldName, part);
				if (step <= 0) {
					throw new IllegalArgumentException("Invalid step in " + fieldName + ": " + part);
				}
			} else {
				rangePart = part;
			}
			final int start;
			final int end;
			if (rangePart.equals("*")) {
				start = min;
				end = max;
			} else if (rangePart.contains("-")) {
				final String[] range = rangePart.split("-", -1);
				if (range.length != 2) {
					throw new IllegalArgumentException("Invalid range in " + fieldName + ": " + part);
				}
				start = parseNumber(range[0], fieldName, part);
				end = parseNumber(range[1], fieldName, part);
				if (start > end) {
					throw new IllegalArgumentException("Invalid range in " + fieldName + ": " + part);
				}
			} else {
				if (slash >= 0) {
					// Vixie cron: a step needs '*' or a range ("5/10" is rejected)
					throw new IllegalArgumentException(
							"Step without '*' or range in " + fieldName + ": " + part + " (use '*/n' or 'a-b/n')");
				}
				start = parseNumber(rangePart, fieldName, part);
				end = start;
			}
			if (start < min || end > max) {
				throw new IllegalArgumentException(
						"Value out of bounds in " + fieldName + ": " + part + " (valid: " + min + "-" + max + ")");
			}
			for (int value = start; value <= end; value += step) {
				allowed[value] = true;
			}
		}
		return allowed;
	}

	private static int parseNumber(final String text, final String fieldName, final String part) {
		try {
			return Integer.parseInt(text);
		} catch (final NumberFormatException ex) {
			throw new IllegalArgumentException("Invalid number in " + fieldName + ": " + part, ex);
		}
	}
}
