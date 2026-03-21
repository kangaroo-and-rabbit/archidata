package org.atriasoft.archidata.tools;

/**
 * Utility class for random string generation.
 */
public class StringTools {
	private StringTools() {
		// Utility class
	}

	/**
	 * Generates a random string of the given length using alphanumeric and special characters.
	 * @param length The desired string length.
	 * @return A random string containing letters, digits, accented characters, and some punctuation.
	 */
	public static String randGeneratedStr(final int length) {
		final String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz0123456789éàê_- '()";
		final StringBuilder out = new StringBuilder(length);
		for (int iii = 0; iii < length; iii++) {
			final int chId = (int) (base.length() * Math.random());
			out.append(base.charAt(chId));
		}
		return out.toString();
	}

	/**
	 * Generates a random URL-safe token string of the given length.
	 * @param length The desired token length.
	 * @return A random string containing only ASCII letters, digits, underscore and hyphen.
	 */
	public static String generateToken(final int length) {
		final String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz0123456789_-";
		final StringBuilder out = new StringBuilder(length);
		for (int iii = 0; iii < length; iii++) {
			final int chId = (int) (base.length() * Math.random());
			out.append(base.charAt(chId));
		}
		return out.toString();
	}

}
