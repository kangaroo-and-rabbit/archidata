package org.atriasoft.archidata.tools;

/**
 * Neutralizes the control characters of a client-controlled value before it is written in a log (log forging).
 *
 * <p>
 * A request path (percent-decoded by JAX-RS), a header or an uploaded file name can carry a real CR/LF: written
 * as is, it lets a caller forge log lines. {@link #clean(Object)} replaces them by a visible escape.
 */
public final class LogSanitizer {

	private LogSanitizer() {
		// utility class
	}

	/**
	 * Returns the text of a value with its control characters escaped: CR, LF and TAB become the two characters
	 * {@code \r}, {@code \n}, {@code \t}; a backslash is doubled, so that an escape sequence cannot be
	 * confused with a literal one; any other control character (ISO control, line and paragraph separators,
	 * Unicode format characters such as bidi overrides and zero-width characters) becomes a backslash followed by {@code u} and four hexadecimal digits.
	 * @param value The value to log (may be {@code null}).
	 * @return {@code "null"} for {@code null}, otherwise the escaped {@link String#valueOf(Object)}.
	 */
	public static String clean(final Object value) {
		final String text = String.valueOf(value);
		final StringBuilder out = new StringBuilder(text.length() + 8);
		boolean changed = false;
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			if (c == '\r') {
				out.append("\\r");
				changed = true;
			} else if (c == '\n') {
				out.append("\\n");
				changed = true;
			} else if (c == '\t') {
				out.append("\\t");
				changed = true;
			} else if (c == '\\') {
				out.append("\\\\");
				changed = true;
			} else if (Character.isISOControl(c) || c == '\u2028' || c == '\u2029'
					|| Character.getType(c) == Character.FORMAT) {
				out.append(String.format("\\u%04x", (int) c));
				changed = true;
			} else {
				out.append(c);
			}
		}
		return changed ? out.toString() : text;
	}
}
