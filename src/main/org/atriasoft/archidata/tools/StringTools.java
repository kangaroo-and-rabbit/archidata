package org.atriasoft.archidata.tools;

public class StringTools {
	private StringTools() {
		// Utility class
	}

	public static String RandGeneratedStr(final int length) {
		final String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz0123456789éàê_- '()";
		final StringBuilder out = new StringBuilder(length);
		for (int iii = 0; iii < length; iii++) {
			final int chId = (int) (base.length() * Math.random());
			out.append(base.charAt(chId));
		}
		return out.toString();
	}

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
