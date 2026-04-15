package test.atriasoft.archidata.apiExtern;

import java.util.Map;
import java.util.UUID;

import org.atriasoft.archidata.tools.JWTWrapper;
import org.junit.jupiter.api.Assertions;

public class Common {
	static {
		try {
			// Generate an ephemeral RSA key pair for test JWT signing/validation
			JWTWrapper.initLocalToken(null);
		} catch (final Exception e) {
			throw new RuntimeException("Failed to init JWT keys for tests", e);
		}
	}

	public static final String USER_TOKEN = JWTWrapper.generateJWToken(16512L, "test_user_login", "Karso",
			"test.atriasoft", //
			Map.of("test.atriasoft", Map.of("USER", Boolean.TRUE)), null, 3600);
	public static final String ADMIN_TOKEN = JWTWrapper.generateJWToken(16512L, "test_admin_login", "Karso",
			"test.atriasoft", Map.of("test.atriasoft", Map.of("USER", Boolean.TRUE, "ADMIN", Boolean.TRUE)), null,
			3600);

	public static String randGeneratedStr(final int length) {
		final String base = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz0123456789éàê_- '()";
		final StringBuilder out = new StringBuilder(length);
		for (int iii = 0; iii < length; iii++) {
			final int chId = (int) (base.length() * Math.random());
			out.append(base.charAt(chId));
		}
		return out.toString();
	}

	public static void checkUUID(final UUID id) {
		final String data = id.toString();
		Assertions.assertFalse(data.equals("00000000-0000-0000-0000-000000000000"));
		final String[] elems = data.split("-");
		Assertions.assertEquals(elems.length, 5);
		Assertions.assertTrue(elems[3].equals("0001"));
	}
}
