package test.atriasoft.archidata.apiExtern;

import java.util.Map;
import java.util.UUID;

import org.atriasoft.archidata.filter.PartRight;
import org.atriasoft.archidata.tools.JWTWrapper;
import org.bson.types.ObjectId;
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

	/** Subject of {@link #USER_TOKEN}: an ObjectId, as {@code AuthenticationFilter} requires. */
	public static final ObjectId USER_ID = new ObjectId("000000000000000000004081");
	/** Subject of {@link #ADMIN_TOKEN}. */
	public static final ObjectId ADMIN_ID = new ObjectId("000000000000000000004082");

	/** Token of a user with the USER role on the test application (roles are PartRight codes). */
	public static final String USER_TOKEN = JWTWrapper.generateJWToken(USER_ID, "test_user_login", "Karso",
			TestAuthenticationFilter.APPLICATION_NAME, //
			Map.of(TestAuthenticationFilter.APPLICATION_NAME, Map.of("USER", PartRight.READ_WRITE.getValue())), null,
			3600);
	/** Token of a user with the USER and ADMIN roles on the test application. */
	public static final String ADMIN_TOKEN = JWTWrapper.generateJWToken(ADMIN_ID, "test_admin_login", "Karso",
			TestAuthenticationFilter.APPLICATION_NAME, //
			Map.of(TestAuthenticationFilter.APPLICATION_NAME,
					Map.of("USER", PartRight.READ_WRITE.getValue(), "ADMIN", PartRight.READ_WRITE.getValue())),
			null, 3600);

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
