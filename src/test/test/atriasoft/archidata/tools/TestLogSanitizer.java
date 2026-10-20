package test.atriasoft.archidata.tools;

import org.atriasoft.archidata.tools.LogSanitizer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestLogSanitizer {

	@Test
	void crLfAreEscaped() {
		Assertions.assertEquals("a\\r\\nINFO forged", LogSanitizer.clean("a\r\nINFO forged"));
	}

	@Test
	void otherControlCharactersAreEscaped() {
		Assertions.assertEquals("x\\ty\\u001bz\\u0000\\u2028", LogSanitizer.clean("x\ty\u001bz\u0000\u2028"));
	}

	@Test
	void backslashIsDoubled() {
		Assertions.assertEquals("a\\\\nb", LogSanitizer.clean("a\\nb"));
		Assertions.assertEquals("a\\\\n\\n", LogSanitizer.clean("a\\n\n"));
	}

	@Test
	void formatCharactersAreEscaped() {
		Assertions.assertEquals("a\\u202eb\\u200bc\\ufeff", LogSanitizer.clean("a\u202eb\u200bc\ufeff"));
	}

	@Test
	void plainTextIsUnchanged() {
		final String text = "/data/thumbnail/é 日本 ok";
		Assertions.assertSame(text, LogSanitizer.clean(text));
	}

	@Test
	void nullAndObjects() {
		Assertions.assertEquals("null", LogSanitizer.clean(null));
		Assertions.assertEquals("12", LogSanitizer.clean(12));
	}
}
