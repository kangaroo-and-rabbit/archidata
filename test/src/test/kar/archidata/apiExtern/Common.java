package test.kar.archidata.apiExtern;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.kar.archidata.tools.JWTWrapper;

public class Common {
	public static final String USER_TOKEN = JWTWrapper.createJwtTestToken(16512, "test_user_login", "KarAuth",
			"farm.neo.back", //
			Map.of("farm.neo.back", Map.of("USER", Boolean.TRUE)));
	public static final String ADMIN_TOKEN = JWTWrapper.createJwtTestToken(16512, "test_admin_login", "KarAuth",
			"farm.neo.back", Map.of("farm.neo.back", Map.of("USER", Boolean.TRUE, "ADMIN", Boolean.TRUE)));

	public static String RandGeneratedStr(final int length) {
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
