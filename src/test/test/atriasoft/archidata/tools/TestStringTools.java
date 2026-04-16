package test.atriasoft.archidata.tools;

import org.atriasoft.archidata.tools.StringTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestStringTools {

	@Test
	void testRandGeneratedStrLength() {
		final String result = StringTools.randGeneratedStr(10);
		Assertions.assertEquals(10, result.length());
	}

	@Test
	void testRandGeneratedStrZeroLength() {
		final String result = StringTools.randGeneratedStr(0);
		Assertions.assertEquals(0, result.length());
	}

	@Test
	void testRandGeneratedStrLargeLength() {
		final String result = StringTools.randGeneratedStr(1000);
		Assertions.assertEquals(1000, result.length());
	}

	@Test
	void testRandGeneratedStrUniqueness() {
		final String result1 = StringTools.randGeneratedStr(50);
		final String result2 = StringTools.randGeneratedStr(50);
		// Extremely unlikely to be equal with 50 random chars
		Assertions.assertNotEquals(result1, result2);
	}

	@Test
	void testGenerateTokenLength() {
		final String result = StringTools.generateToken(20);
		Assertions.assertEquals(20, result.length());
	}

	@Test
	void testGenerateTokenZeroLength() {
		final String result = StringTools.generateToken(0);
		Assertions.assertEquals(0, result.length());
	}

	@Test
	void testGenerateTokenContainsOnlyValidChars() {
		final String result = StringTools.generateToken(500);
		final String validChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz0123456789_-";
		for (int i = 0; i < result.length(); i++) {
			Assertions.assertTrue(validChars.indexOf(result.charAt(i)) >= 0,
					"Invalid character found: '" + result.charAt(i) + "' at position " + i);
		}
	}

	@Test
	void testGenerateTokenUniqueness() {
		final String result1 = StringTools.generateToken(50);
		final String result2 = StringTools.generateToken(50);
		Assertions.assertNotEquals(result1, result2);
	}
}
