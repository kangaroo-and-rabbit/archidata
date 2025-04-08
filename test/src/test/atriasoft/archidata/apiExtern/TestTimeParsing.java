package test.atriasoft.archidata.apiExtern;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.TimeZone;

import org.atriasoft.archidata.tools.DateTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestTimeParsing {
	private final static Logger LOGGER = LoggerFactory.getLogger(TestTime.class);

	@BeforeAll
	public static void setUp() {
		// Set default timezone to UTC
		//TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	//	@Test
	//	public void testRaw() throws Exception {
	//		LOGGER.info("=======================================================================");
	//		String data = null;
	//		OffsetDateTime parsed = null;
	//		String manualFormat = null;
	//
	//		data = "2025-04-04T15:15:07.123Z";
	//		//parsed = OffsetDateTime.parse(data, timeParserPerso);
	//		parsed = DateTools.parseOffsetDateTime(data);
	//		LOGGER.info(">> send        : '{}'", data);
	//		LOGGER.info(">> parsed      : '{}'", parsed);
	//		manualFormat = parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSSX"));
	//		LOGGER.info(">> manualFormat: '{}'", manualFormat);
	//		manualFormat = parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'nnnnnnnnnZ"));
	//		LOGGER.info(">> manualFormat: '{}'", manualFormat);
	//		LOGGER.info("----------------------------------------------------");
	//		data = "2025-04-04T15:15:07.123456789+05:00";
	//		//parsed = OffsetDateTime.parse(data, timeParserPerso);
	//		parsed = DateTools.parseOffsetDateTime(data);
	//		LOGGER.info(">> send        : '{}'", data);
	//		LOGGER.info(">> parsed      : '{}'", parsed);
	//		manualFormat = parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSSX"));
	//		LOGGER.info(">> manualFormat: '{}'", manualFormat);
	//		manualFormat = parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'nnnnnnnnnZ"));
	//		LOGGER.info(">> manualFormat: '{}'", manualFormat);
	//		LOGGER.info("----------------------------------------------------");
	//		data = "2025-04-04 15:15:07.123456789";
	//		//parsed = OffsetDateTime.parse(data, timeParserPersoUTC);
	//		parsed = DateTools.parseOffsetDateTime(data);
	//		LOGGER.info(">> send        : '{}'", data);
	//		LOGGER.info(">> parsed      : '{}'", parsed);
	//		manualFormat = parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSSX"));
	//		LOGGER.info(">> manualFormat: '{}'", manualFormat);
	//		manualFormat = parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'nnnnnnnnnZ"));
	//		LOGGER.info(">> manualFormat: '{}'", manualFormat);
	//		LOGGER.info("----------------------------------------------------");
	//		data = "2025-04-04 15:15:07";
	//		//parsed = OffsetDateTime.parse(data, timeParserPersoUTC);
	//		parsed = DateTools.parseOffsetDateTime(data);
	//		LOGGER.info(">> send        : '{}'", data);
	//		LOGGER.info(">> parsed      : '{}'", parsed);
	//		manualFormat = parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSSX"));
	//		LOGGER.info(">> manualFormat: '{}'", manualFormat);
	//		manualFormat = parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'nnnnnnnnnZ"));
	//		LOGGER.info(">> manualFormat: '{}'", ma nualFormat);
	//		LOGGER.info("----------------------------------------------------");
	//		data = "2025-04-04T15:15:07";
	//		//parsed = OffsetDateTime.parse(data, timeParserPersoUTC);
	//		parsed = DateTools.parseOffsetDateTime(data);
	//		LOGGER.info(">> send        : '{}'", data);
	//		LOGGER.info(">> parsed      : '{}'", parsed);
	//		manualFormat = parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'SSSX"));
	//		LOGGER.info(">> manualFormat: '{}'", manualFormat);
	//		manualFormat = parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'.'nnnnnnnnnZ"));
	//		LOGGER.info(">> manualFormat: '{}'", manualFormat);
	//		LOGGER.info("----------------------------------------------------");
	//
	//	}

	@Test
	public void testDateRaw() throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
		LOGGER.info("=======================================================================");
		String data = null;
		Date parsed = null;
		data = "1999-01-30T18:16:17.123Z";
		parsed = DateTools.parseDate(data);
		Assertions.assertEquals("1999-01-31T03:16:17.123+09:00", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		Assertions.assertEquals("1999-01-30T18:16:17.123Z", DateTools.serializeMilliWithUTCTimeZone(parsed));
		data = "1999-01-30T18:16:17.123456789Z";
		parsed = DateTools.parseDate(data);
		Assertions.assertEquals("1999-01-30T18:16:17.123000000Z", DateTools.serializeNanoWithUTCTimeZone(parsed));
		data = "1999-01-30T18:16:17.123456789+05:00";
		parsed = DateTools.parseDate(data);
		Assertions.assertEquals("1999-01-30T22:16:17.123+09:00", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		Assertions.assertEquals("1999-01-30T13:16:17.123Z", DateTools.serializeMilliWithUTCTimeZone(parsed));
		data = "1999-01-30T18:16:17.123456789";
		parsed = DateTools.parseDate(data);
		Assertions.assertEquals("1999-01-31T03:16:17.123+09:00", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		Assertions.assertEquals("1999-01-30T18:16:17.123Z", DateTools.serializeMilliWithUTCTimeZone(parsed));
		data = "1999-01-30 18:16:17";
		parsed = DateTools.parseDate(data);
		Assertions.assertEquals("1999-01-31T03:16:17.000+09:00", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		Assertions.assertEquals("1999-01-30T18:16:17.000Z", DateTools.serializeMilliWithUTCTimeZone(parsed));
		data = "1999-01-30T18:16:17";
		parsed = DateTools.parseDate(data);
		Assertions.assertEquals("1999-01-31T03:16:17.000+09:00", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		Assertions.assertEquals("1999-01-30T18:16:17.000Z", DateTools.serializeMilliWithUTCTimeZone(parsed));
		data = "1999-01-30T18:16:17.123+09:00";
		parsed = DateTools.parseDate(data);
		Assertions.assertEquals("1999-01-30T18:16:17.123+09:00", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		Assertions.assertEquals("1999-01-30T09:16:17.123Z", DateTools.serializeMilliWithUTCTimeZone(parsed));
		data = "1999-01-30T18:16:17.123+09";
		parsed = DateTools.parseDate(data);
		Assertions.assertEquals("1999-01-30T18:16:17.123+09:00", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		Assertions.assertEquals("1999-01-30T09:16:17.123Z", DateTools.serializeMilliWithUTCTimeZone(parsed));

		Assertions.assertThrows(IOException.class, () -> DateTools.parseOffsetDateTime("1999-01-30"));
		Assertions.assertThrows(IOException.class, () -> DateTools.parseOffsetDateTime("18:16:17.123"));
	}

	@Test
	public void testOffsetDateTime() throws Exception {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
		LOGGER.info("=======================================================================");
		String data = null;
		OffsetDateTime parsed = null;
		data = "1999-01-30T18:16:17.123Z";
		parsed = DateTools.parseOffsetDateTime(data);
		Assertions.assertEquals("1999-01-30T18:16:17.123Z", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		Assertions.assertEquals("1999-01-30T18:16:17.123Z", DateTools.serializeMilliWithUTCTimeZone(parsed));
		data = "1999-01-30T18:16:17.123456789Z";
		parsed = DateTools.parseOffsetDateTime(data);
		Assertions.assertEquals("1999-01-30T18:16:17.123456789Z", DateTools.serializeNanoWithUTCTimeZone(parsed));
		data = "1999-01-30T18:16:17.123456789+05:00";
		parsed = DateTools.parseOffsetDateTime(data);
		Assertions.assertEquals("1999-01-30T18:16:17.123+05:00", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		Assertions.assertEquals("1999-01-30T13:16:17.123Z", DateTools.serializeMilliWithUTCTimeZone(parsed));
		data = "1999-01-30T18:16:17.123+09:00";
		parsed = DateTools.parseOffsetDateTime(data);
		Assertions.assertEquals("1999-01-30T18:16:17.123+09:00", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		Assertions.assertEquals("1999-01-30T09:16:17.123Z", DateTools.serializeMilliWithUTCTimeZone(parsed));
		data = "1999-01-30T18:16:17.123+09";
		parsed = DateTools.parseOffsetDateTime(data);
		Assertions.assertEquals("1999-01-30T18:16:17.123+09:00", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		Assertions.assertEquals("1999-01-30T09:16:17.123Z", DateTools.serializeMilliWithUTCTimeZone(parsed));

		Assertions.assertThrows(IOException.class,
				() -> DateTools.parseOffsetDateTime("1999-01-30T18:16:17.123456789"));
		Assertions.assertThrows(IOException.class, () -> DateTools.parseOffsetDateTime("1999-01-30 18:16:17"));
		Assertions.assertThrows(IOException.class, () -> DateTools.parseOffsetDateTime("1999-01-30T18:16:17"));
		Assertions.assertThrows(IOException.class, () -> DateTools.parseOffsetDateTime("1999-01-30"));
		Assertions.assertThrows(IOException.class, () -> DateTools.parseOffsetDateTime("18:16:17.123"));

		//		data = "1999-01-30T18:16:17.123 UTC+09:00";
		//		parsed = DateTools.parseDate2(data);
		//		LOGGER.info(">> send      : '{}'", data);
		//		LOGGER.info(">> format OTZ: '{}'", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		//		LOGGER.info(">> format UTC: '{}'", DateTools.serializeMilliWithUTCTimeZone(parsed));
		//		LOGGER.info("----------------------------------------------------");
		//		Assertions.assertEquals("", DateTools.serializeMilliWithOriginalTimeZone(parsed));
		//		Assertions.assertEquals("", DateTools.serializeMilliWithUTCTimeZone(parsed));
	}
}
