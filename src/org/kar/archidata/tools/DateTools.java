package org.kar.archidata.tools;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateTools {
	private final static Logger LOGGER = LoggerFactory.getLogger(DateTools.class);

	/**
	 * Parses a date string using a given pattern into a java.util.Date.
	 *
	 * @param inputDate the string to parse
	 * @param pattern the pattern to use (e.g., "yyyy-MM-dd")
	 * @return parsed Date object
	 * @throws ParseException if parsing fails
	 */
	@Deprecated
	public static Date parseDate(final String inputDate, final String pattern) throws ParseException {
		final SimpleDateFormat format = new SimpleDateFormat(pattern);
		return format.parse(inputDate);
	}

	/**
	 * Formats a java.util.Date using the specified format pattern.
	 *
	 * @param date the Date to format
	 * @param requiredDateFormat the format string (e.g., "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
	 * @return formatted string
	 */
	@Deprecated
	public static String formatDate(final Date date, final String requiredDateFormat) {
		final SimpleDateFormat df = new SimpleDateFormat(requiredDateFormat);
		return df.format(date);
	}

	/**
	 * Formats a java.util.Date using a default ISO 8601-like format.
	 *
	 * @param date the Date to format
	 * @return formatted string in pattern "yyyy-MM-dd'T'HH:mm.ss.SSS'Z'"
	 */
	@Deprecated
	public static String formatDate(final Date date) {
		return serializeMilliWithUTCTimeZone(date);
	}

	// List of supported parsers for flexible date string parsing.
	// Includes patterns with optional parts, slashes, and ISO standard formats.
	static final List<DateTimeFormatter> FORMATTERS = Arrays.asList(
			DateTimeFormatter.ofPattern("[yyyy[-MM[-dd]]]['T'][' '][HH[:mm[:ss['.'nnnnnnnnn]]]]XXXXX"),
			DateTimeFormatter.ofPattern("[yyyy[/MM[/dd]]]['T'][' '][HH[:mm[:ss['.'nnnnnnnnn]]]]XXXXX"),
			DateTimeFormatter.ISO_OFFSET_DATE_TIME, // e.g., 2025-04-04T15:30:00+02:00
			DateTimeFormatter.ISO_ZONED_DATE_TIME, // e.g., 2025-04-04T15:30:00+02:00[Europe/Paris]
			DateTimeFormatter.ISO_INSTANT // e.g., 2025-04-04T13:30:00Z
	);

	/**
	 * Attempts to parse a date string into an OffsetDateTime using a flexible list of patterns.
	 * Supports ISO 8601 formats, optional zone, and fallback to LocalDate or LocalTime if needed.
	 *
	 * @param dateString the date string to parse
	 * @return OffsetDateTime representation of the parsed input
	 * @throws IOException if no supported format matches the input
	 */
	public static OffsetDateTime parseOffsetDateTime(final String dateString) throws IOException {
		for (final DateTimeFormatter formatter : FORMATTERS) {
			try {
				return OffsetDateTime.parse(dateString, formatter);
			} catch (final DateTimeParseException ex) {
				// If the date string is missing a zone, try appending "Z"
				try {
					if (dateString.endsWith("Z") || dateString.endsWith("z")) {
						continue;
					}
					return OffsetDateTime.parse(dateString + "Z", formatter);
				} catch (final DateTimeParseException ex2) {
					// Still failed, try next pattern
				}
			}
		}

		// Fallback: Try parsing as LocalDate (assume start of day UTC)
		try {
			final LocalDate dateTmp = LocalDate.parse(dateString);
			return dateTmp.atStartOfDay(ZoneOffset.UTC).toInstant().atZone(ZoneOffset.UTC).toOffsetDateTime();
		} catch (final DateTimeParseException e) {
			// Ignore and try next fallback
		}

		// Fallback: Try parsing as LocalTime (assume date is 0000-01-01 UTC)
		try {
			final LocalTime timeTmp = LocalTime.parse(dateString);
			return OffsetDateTime.of(0, 1, 1, // year, month, day
					timeTmp.getHour(), timeTmp.getMinute(), timeTmp.getSecond(), timeTmp.getNano(), ZoneOffset.UTC);
		} catch (final DateTimeParseException e) {
			// All parsing attempts failed
		}

		throw new IOException("Unrecognized DATE format: '" + dateString + "' supported format ISO8601");
	}

	/**
	 * Parses a flexible date string and returns a java.util.Date,
	 * using system default timezone for conversion.
	 *
	 * @param dateString the input string to parse
	 * @return java.util.Date object
	 * @throws ParseException if parsing fails entirely
	 */
	public static Date parseDate(final String dateString) throws IOException {
		final OffsetDateTime dateTime = parseOffsetDateTime(dateString);
		dateTime.atZoneSameInstant(ZoneId.systemDefault());
		return Date.from(dateTime.toInstant());
	}

	/**
	 *  Formatter for date-time with milliseconds and original timezone offset (e.g., 2025-04-06T15:00:00.123+02:00)
	 */
	public static final DateTimeFormatter PATTERN_MS_TIME_WITH_ZONE = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSSXXX");

	/**
	 * Serializes an OffsetDateTime to a string including milliseconds and the original timezone offset.
	 * Example output: 2025-04-06T15:00:00.123+02:00
	 */
	public static String serializeMilliWithOriginalTimeZone(final OffsetDateTime offsetDateTime) {
		return offsetDateTime.format(PATTERN_MS_TIME_WITH_ZONE);
	}

	/**
	 * Converts a java.util.Date to OffsetDateTime using the system's default timezone,
	 * then serializes it to a string with milliseconds and original timezone offset.
	 */
	public static String serializeMilliWithOriginalTimeZone(final Date date) {
		return serializeMilliWithOriginalTimeZone(date.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime());
	}

	/**
	 * Formatter for date-time with milliseconds in UTC offset (e.g., 2025-04-06T13:00:00.123Z)
	 */
	public static final DateTimeFormatter PATTERN_MS_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSSX");

	/**
	 * Serializes an OffsetDateTime to a string with milliseconds in UTC.
	 * The offset is explicitly changed to UTC before formatting.
	 */
	public static String serializeMilliWithUTCTimeZone(final OffsetDateTime offsetDateTime) {
		return offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).format(PATTERN_MS_TIME);
	}

	/**
	 * Converts a java.util.Date to OffsetDateTime in the system's default timezone,
	 * then serializes it with milliseconds in UTC.
	 */
	public static String serializeMilliWithUTCTimeZone(final Date date) {
		return serializeMilliWithUTCTimeZone(date.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime());
	}

	/**
	 * Formatter for date-time with nanoseconds in UTC offset (e.g., 2025-04-06T13:00:00.123456789Z)
	 */
	public static final DateTimeFormatter PATTERN_NS_TIME = DateTimeFormatter
			.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'nnnnnnnnnX");

	/**
	 * Serializes an OffsetDateTime to a string with nanosecond precision in UTC.
	 */
	public static String serializeNanoWithUTCTimeZone(final OffsetDateTime offsetDateTime) {
		return offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).format(PATTERN_NS_TIME);
	}

	/**
	 * Converts a java.util.Date to OffsetDateTime in the system's default timezone,
	 * then serializes it with nanoseconds in UTC.
	 */
	public static String serializeNanoWithUTCTimeZone(final Date date) {
		return serializeNanoWithUTCTimeZone(date.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime());
	}
}
