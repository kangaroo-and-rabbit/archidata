package test.atriasoft.archidata.apiExtern;

import org.atriasoft.archidata.filter.AuthenticationFilter;
import org.atriasoft.archidata.filter.MySecurityContext;
import org.atriasoft.archidata.model.UserByToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The shared test tokens of {@link Common} must be accepted by the {@link AuthenticationFilter} used
 * by the test server ({@link TestAuthenticationFilter}) and carry the roles they claim.
 */
class TestCommonTokens {

	/** Exposes the protected JWT validation of the test filter. */
	private static final class ExposedFilter extends TestAuthenticationFilter {
		UserByToken validate(final String token) throws Exception {
			return validateJwtToken(token);
		}
	}

	private static MySecurityContext contextOf(final String token) throws Exception {
		final UserByToken user = new ExposedFilter().validate(token);
		Assertions.assertNotNull(user);
		Assertions.assertNotNull(user.getOid());
		return new MySecurityContext(user, "http");
	}

	@Test
	void testAdminTokenIsAcceptedWithAdminRole() throws Exception {
		final MySecurityContext context = contextOf(Common.ADMIN_TOKEN);
		Assertions.assertTrue(context.isUserInRole(TestAuthenticationFilter.APPLICATION_NAME + "/ADMIN"));
		Assertions.assertTrue(context.isUserInRole(TestAuthenticationFilter.APPLICATION_NAME + "/USER"));
	}

	@Test
	void testUserTokenIsAcceptedWithoutAdminRole() throws Exception {
		final MySecurityContext context = contextOf(Common.USER_TOKEN);
		Assertions.assertTrue(context.isUserInRole(TestAuthenticationFilter.APPLICATION_NAME + "/USER"));
		Assertions.assertFalse(context.isUserInRole(TestAuthenticationFilter.APPLICATION_NAME + "/ADMIN"));
	}
}
