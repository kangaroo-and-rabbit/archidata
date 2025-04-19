package org.atriasoft.archidata.tools;

import java.io.IOException;
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
		return parseOffsetDateTime(dateString, false);
	}

	/**
	 * Attempts to parse a date string into an OffsetDateTime using a flexible list of patterns.
	 * Supports ISO 8601 formats, optional zone, and fallback to LocalDate or LocalTime if needed.
	 *
	 * @param dateString the date string to parse
	 * @param missingAsUTC Parse date when missing the time zone consider it as a UTC Date-time
	 * @return OffsetDateTime representation of the parsed input
	 * @throws IOException if no supported format matches the input
	 */
	public static OffsetDateTime parseOffsetDateTime(final String dateString, final boolean missingAsUTC)
			throws IOException {
		if (dateString == null) {
			return null;
		}
		for (final DateTimeFormatter formatter : FORMATTERS) {
			try {
				return OffsetDateTime.parse(dateString, formatter);
			} catch (final DateTimeParseException ex) {
				if (missingAsUTC) {
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
		}
		throw new IOException("Unrecognized DATE format: '" + dateString + "' supported format ISO8601");
	}

	/**
	 * Parses a flexible date string and returns a java.util.Date,
	 * using system default time-zone for conversion.
	 *
	 * @param dateString the input string to parse
	 * @return The parsed Date
	 * @throws IOException if parsing fails.
	 */
	public static Date parseDate(final String dateString) throws IOException {
		final OffsetDateTime dateTime = parseOffsetDateTime(dateString, true);
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
		if (offsetDateTime == null) {
			return null;
		}
		return offsetDateTime.format(PATTERN_MS_TIME_WITH_ZONE);
	}

	/**
	 * Converts a java.util.Date to OffsetDateTime using the system's default timezone,
	 * then serializes it to a string with milliseconds and original timezone offset.
	 */
	public static String serializeMilliWithOriginalTimeZone(final Date date) {
		if (date == null) {
			return null;
		}
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
		if (offsetDateTime == null) {
			return null;
		}
		return offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).format(PATTERN_MS_TIME);
	}

	/**
	 * Converts a java.util.Date to OffsetDateTime in the system's default timezone,
	 * then serializes it with milliseconds in UTC.
	 */
	public static String serializeMilliWithUTCTimeZone(final Date date) {
		if (date == null) {
			return null;
		}
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
		if (offsetDateTime == null) {
			return null;
		}
		return offsetDateTime.withOffsetSameInstant(ZoneOffset.UTC).format(PATTERN_NS_TIME);
	}

	/**
	 * Converts a java.util.Date to OffsetDateTime in the system's default timezone,
	 * then serializes it with nanoseconds in UTC.
	 */
	public static String serializeNanoWithUTCTimeZone(final Date date) {
		if (date == null) {
			return null;
		}
		return serializeNanoWithUTCTimeZone(date.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime());
	}
}
