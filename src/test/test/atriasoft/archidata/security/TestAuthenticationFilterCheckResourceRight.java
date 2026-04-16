package test.atriasoft.archidata.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.atriasoft.archidata.filter.AuthenticationFilter;
import org.atriasoft.archidata.filter.MySecurityContext;
import org.atriasoft.archidata.filter.PartRight;
import org.atriasoft.archidata.model.UserByToken;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test suite for {@link AuthenticationFilter#checkResourceRight(MySecurityContext, String, PartRight)} method.
 *
 * <p>
 * Verifies that the fine-grained resource-level rights checking works correctly
 * with bitmask logic: {@code (userRight &amp; required) == required}.
 * </p>
 */
public class TestAuthenticationFilterCheckResourceRight {

	private static final String APPLICATION_NAME = "myapp";
	private TestableAuthenticationFilter authFilter;
	private MySecurityContext securityContext;
	private UserByToken user;

	/**
	 * Testable subclass that exposes checkResourceRight() as public.
	 */
	private static class TestableAuthenticationFilter extends AuthenticationFilter {
		public TestableAuthenticationFilter(final String applicationName) {
			super(applicationName);
		}

		@Override
		public boolean checkResourceRight(
				final MySecurityContext userContext,
				final String rightName,
				final PartRight requiredAccess) {
			return super.checkResourceRight(userContext, rightName, requiredAccess);
		}
	}

	@BeforeEach
	public void setup() {
		authFilter = new TestableAuthenticationFilter(APPLICATION_NAME);

		user = new UserByToken();
		user.setOid(new ObjectId());
		user.setName("testuser");
		user.setType(UserByToken.TYPE_USER);

		// Setup fine-grained rights for "myapp" application:
		// - articles: READ_WRITE
		// - users: READ
		// - logs: WRITE
		final Map<String, Map<String, PartRight>> rights = new HashMap<>();
		final Map<String, PartRight> myappRights = new HashMap<>();
		myappRights.put("articles", PartRight.READ_WRITE);
		myappRights.put("users", PartRight.READ);
		myappRights.put("logs", PartRight.WRITE);
		rights.put(APPLICATION_NAME, myappRights);
		user.setRight(rights);

		// Also set some roles (separate from rights)
		final Map<String, Map<String, PartRight>> roles = new HashMap<>();
		final Map<String, PartRight> myappRoles = new HashMap<>();
		myappRoles.put("ADMIN", PartRight.READ_WRITE);
		roles.put(APPLICATION_NAME, myappRoles);
		user.setRoles(roles);

		securityContext = new MySecurityContext(user, "https");
	}

	// ========================================================================
	// Test READ_WRITE right
	// ========================================================================

	@Test
	public void testResourceRight_ReadWrite_RequireRead() {
		// User has articles:READ_WRITE, required READ → OK (3 & 1 == 1)
		assertTrue(authFilter.checkResourceRight(securityContext, "articles", PartRight.READ));
	}

	@Test
	public void testResourceRight_ReadWrite_RequireWrite() {
		// User has articles:READ_WRITE, required WRITE → OK (3 & 2 == 2)
		assertTrue(authFilter.checkResourceRight(securityContext, "articles", PartRight.WRITE));
	}

	@Test
	public void testResourceRight_ReadWrite_RequireReadWrite() {
		// User has articles:READ_WRITE, required READ_WRITE → OK (3 & 3 == 3)
		assertTrue(authFilter.checkResourceRight(securityContext, "articles", PartRight.READ_WRITE));
	}

	// ========================================================================
	// Test READ-only right
	// ========================================================================

	@Test
	public void testResourceRight_ReadOnly_RequireRead() {
		// User has users:READ, required READ → OK (1 & 1 == 1)
		assertTrue(authFilter.checkResourceRight(securityContext, "users", PartRight.READ));
	}

	@Test
	public void testResourceRight_ReadOnly_RequireWrite() {
		// User has users:READ, required WRITE → FAIL (1 & 2 == 0 != 2)
		assertFalse(authFilter.checkResourceRight(securityContext, "users", PartRight.WRITE));
	}

	@Test
	public void testResourceRight_ReadOnly_RequireReadWrite() {
		// User has users:READ, required READ_WRITE → FAIL (1 & 3 == 1 != 3)
		assertFalse(authFilter.checkResourceRight(securityContext, "users", PartRight.READ_WRITE));
	}

	// ========================================================================
	// Test WRITE-only right
	// ========================================================================

	@Test
	public void testResourceRight_WriteOnly_RequireRead() {
		// User has logs:WRITE, required READ → FAIL (2 & 1 == 0 != 1)
		assertFalse(authFilter.checkResourceRight(securityContext, "logs", PartRight.READ));
	}

	@Test
	public void testResourceRight_WriteOnly_RequireWrite() {
		// User has logs:WRITE, required WRITE → OK (2 & 2 == 2)
		assertTrue(authFilter.checkResourceRight(securityContext, "logs", PartRight.WRITE));
	}

	@Test
	public void testResourceRight_WriteOnly_RequireReadWrite() {
		// User has logs:WRITE, required READ_WRITE → FAIL (2 & 3 == 2 != 3)
		assertFalse(authFilter.checkResourceRight(securityContext, "logs", PartRight.READ_WRITE));
	}

	// ========================================================================
	// Test non-existent right
	// ========================================================================

	@Test
	public void testResourceRight_NonExistent_RequireRead() {
		// User has no "comments" right → FAIL
		assertFalse(authFilter.checkResourceRight(securityContext, "comments", PartRight.READ));
	}

	@Test
	public void testResourceRight_NonExistent_RequireWrite() {
		assertFalse(authFilter.checkResourceRight(securityContext, "comments", PartRight.WRITE));
	}

	// ========================================================================
	// Test with different application name
	// ========================================================================

	@Test
	public void testResourceRight_DifferentApplication() {
		// Create filter for "otherapp" — user has no rights for "otherapp"
		final TestableAuthenticationFilter otherAuthFilter = new TestableAuthenticationFilter("otherapp");
		assertFalse(otherAuthFilter.checkResourceRight(securityContext, "articles", PartRight.READ));
	}

	// ========================================================================
	// Test NONE access
	// ========================================================================

	@Test
	public void testResourceRight_RequireNone() {
		// NONE (0) — (any & 0 == 0), should always pass if right exists
		assertTrue(authFilter.checkResourceRight(securityContext, "articles", PartRight.NONE));
	}

	@Test
	public void testResourceRight_RequireNone_NonExistent() {
		// NONE on non-existent right → FAIL (no right at all)
		assertFalse(authFilter.checkResourceRight(securityContext, "comments", PartRight.NONE));
	}

	// ========================================================================
	// Test user with no rights (empty)
	// ========================================================================

	@Test
	public void testResourceRight_EmptyUser() {
		final UserByToken emptyUser = new UserByToken();
		emptyUser.setOid(new ObjectId());
		emptyUser.setName("emptyuser");
		emptyUser.setType(UserByToken.TYPE_USER);

		final MySecurityContext emptyContext = new MySecurityContext(emptyUser, "https");
		assertFalse(authFilter.checkResourceRight(emptyContext, "articles", PartRight.READ));
	}

	// ========================================================================
	// Test that roles and rights are independent
	// ========================================================================

	@Test
	public void testResourceRight_RolesDoNotInterfere() {
		// User has role ADMIN but no right named "ADMIN"
		// checkResourceRight should NOT match roles
		assertFalse(authFilter.checkResourceRight(securityContext, "ADMIN", PartRight.READ));
	}
}
