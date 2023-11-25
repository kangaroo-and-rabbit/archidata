package org.kar.archidata.tools;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DateTools {
	static private List<SimpleDateFormat> knownPatterns = new ArrayList<>();
	{
		// SYSTEM mode
		DateTools.knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"));
		DateTools.knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.ss.SSS'Z'"));
		DateTools.knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
		DateTools.knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS"));
		// Human mode
		DateTools.knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss"));
		DateTools.knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS"));
		// date mode
		DateTools.knownPatterns.add(new SimpleDateFormat("yyyy-MM-dd"));
		// time mode
		DateTools.knownPatterns.add(new SimpleDateFormat("HH:mm:ss"));
		DateTools.knownPatterns.add(new SimpleDateFormat("HH:mm:ss.SSS"));
	}

	public static Date parseDate(final String inputDate, final String pattern) throws ParseException {
		final SimpleDateFormat format = new SimpleDateFormat(pattern);
		return format.parse(inputDate);
	}

	public static Date parseDate(final String inputDate) throws ParseException {
		for (final SimpleDateFormat pattern : DateTools.knownPatterns) {
			try {
				return pattern.parse(inputDate);
			} catch (final ParseException e) {
				continue;
			}
		}
		throw new ParseException("Can not parse the date-time format: '" + inputDate + "'", 0);

	}

	public static String formatDate(final Date date, final String requiredDateFormat) {
		final SimpleDateFormat df = new SimpleDateFormat(requiredDateFormat);
		final String outputDateFormatted = df.format(date);
		return outputDateFormatted;
	}

	public static String formatDate(final Date date) {
		return formatDate(date, "yyyy-MM-dd'T'HH:mm.ss.SSS'Z'");
	}
}
