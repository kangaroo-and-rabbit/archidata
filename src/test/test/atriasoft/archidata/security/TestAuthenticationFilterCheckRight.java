package test.atriasoft.archidata.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.exception.SystemException;
import org.atriasoft.archidata.filter.AuthenticationFilter;
import org.atriasoft.archidata.filter.MySecurityContext;
import org.atriasoft.archidata.filter.PartRight;
import org.atriasoft.archidata.model.UserByToken;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Test suite for {@link AuthenticationFilter#checkRight(ContainerRequestContext, MySecurityContext, List)} method.
 *
 * <h2>Purpose</h2>
 * This class verifies that the default {@code AuthenticationFilter} implementation correctly:
 * <ul>
 * <li>Prefixes all roles with {@code applicationName + "/"}</li>
 * <li>Delegates to {@code MySecurityContext.isUserInRole()}</li>
 * <li>Returns {@code true} if ANY role in the list matches</li>
 * </ul>
 *
 * <h2>Default Behavior</h2>
 * The base {@code AuthenticationFilter.checkRight()} implementation automatically prefixes
 * all roles from {@code @RolesAllowed} with the application name:
 *
 * <pre>
 * @RolesAllowed("ADMIN")        → checks "myapp/ADMIN"
 * @RolesAllowed("EDITOR:w")     → checks "myapp/EDITOR:w"
 * @RolesAllowed({"A", "B"})     → checks "myapp/A" OR "myapp/B"
 * </pre>
 *
 * <h2>JWT Structure</h2>
 * For the default implementation to work, JWT tokens should include rights
 * structured by application name:
 *
 * <pre>
 * {
 *   "right": {
 *     "myapp": {
 *       "ADMIN": "READ_WRITE",
 *       "EDITOR": "WRITE"
 *     }
 *   }
 * }
 * </pre>
 *
 * <h2>Note for Custom Implementations</h2>
 * Applications can override {@code checkRight()} to implement custom logic
 * (e.g., handling {@code {entity}/ROLE} patterns). See {@code NeoAuthenticationFilter}
 * in farm.neo.back for an example.
 */
public class TestAuthenticationFilterCheckRight {

	private static final String APPLICATION_NAME = "myapp";
	private TestableAuthenticationFilter authFilter;
	private ContainerRequestContext requestContext;
	private MySecurityContext securityContext;
	private UserByToken user;

	/**
	 * Testable subclass that exposes checkRight() as public
	 */
	private static class TestableAuthenticationFilter extends AuthenticationFilter {
		public TestableAuthenticationFilter(final String applicationName) {
			super(applicationName);
		}

		@Override
		public boolean checkRight(
				final ContainerRequestContext requestContext,
				final MySecurityContext userContext,
				final List<String> roles) throws SystemException {
			return super.checkRight(requestContext, userContext, roles);
		}
	}

	@BeforeEach
	public void setup() {
		authFilter = new TestableAuthenticationFilter(APPLICATION_NAME);
		requestContext = null; // Not used in base implementation of checkRight()

		user = new UserByToken();
		user.setOid(new ObjectId());
		user.setName("testuser");
		user.setType(UserByToken.TYPE_USER);

		// Setup rights for "myapp" application:
		// - myapp/ADMIN: READ_WRITE
		// - myapp/EDITOR: WRITE
		// - myapp/VIEWER: READ
		final Map<String, Map<String, PartRight>> rights = new HashMap<>();

		final Map<String, PartRight> myappRights = new HashMap<>();
		myappRights.put("ADMIN", PartRight.READ_WRITE);
		myappRights.put("EDITOR", PartRight.WRITE);
		myappRights.put("VIEWER", PartRight.READ);
		rights.put(APPLICATION_NAME, myappRights);

		user.setRight(rights);
		securityContext = new MySecurityContext(user, "https");
	}

	// ========================================================================
	// Test single role matching
	// ========================================================================

	@Test
	public void testCheckRight_SingleRole_Match() throws SystemException {
		// @RolesAllowed("ADMIN") → checks "myapp/ADMIN"
		final List<String> roles = Arrays.asList("ADMIN");
		assertTrue(authFilter.checkRight(requestContext, securityContext, roles));
	}

	@Test
	public void testCheckRight_SingleRole_NoMatch() throws SystemException {
		// @RolesAllowed("UNKNOWN") → checks "myapp/UNKNOWN"
		final List<String> roles = Arrays.asList("UNKNOWN");
		assertFalse(authFilter.checkRight(requestContext, securityContext, roles));
	}

	@Test
	public void testCheckRight_SingleRole_WithSuffix() throws SystemException {
		// @RolesAllowed("EDITOR:w") → checks "myapp/EDITOR:w"
		final List<String> roles = Arrays.asList("EDITOR:w");
		assertTrue(authFilter.checkRight(requestContext, securityContext, roles));
	}

	@Test
	public void testCheckRight_SingleRole_WithSuffixNoMatch() throws SystemException {
		// @RolesAllowed("VIEWER:w") → checks "myapp/VIEWER:w"
		// User has VIEWER:READ, not WRITE
		final List<String> roles = Arrays.asList("VIEWER:w");
		assertFalse(authFilter.checkRight(requestContext, securityContext, roles));
	}

	// ========================================================================
	// Test multiple roles (OR logic)
	// ========================================================================

	@Test
	public void testCheckRight_MultipleRoles_FirstMatches() throws SystemException {
		// @RolesAllowed({"ADMIN", "UNKNOWN"}) → checks "myapp/ADMIN" OR "myapp/UNKNOWN"
		final List<String> roles = Arrays.asList("ADMIN", "UNKNOWN");
		assertTrue(authFilter.checkRight(requestContext, securityContext, roles));
	}

	@Test
	public void testCheckRight_MultipleRoles_SecondMatches() throws SystemException {
		// @RolesAllowed({"UNKNOWN", "ADMIN"}) → checks "myapp/UNKNOWN" OR "myapp/ADMIN"
		final List<String> roles = Arrays.asList("UNKNOWN", "ADMIN");
		assertTrue(authFilter.checkRight(requestContext, securityContext, roles));
	}

	@Test
	public void testCheckRight_MultipleRoles_NoneMatch() throws SystemException {
		// @RolesAllowed({"UNKNOWN1", "UNKNOWN2"}) → checks "myapp/UNKNOWN1" OR "myapp/UNKNOWN2"
		final List<String> roles = Arrays.asList("UNKNOWN1", "UNKNOWN2");
		assertFalse(authFilter.checkRight(requestContext, securityContext, roles));
	}

	@Test
	public void testCheckRight_MultipleRoles_AllMatch() throws SystemException {
		// @RolesAllowed({"ADMIN", "EDITOR"}) → checks "myapp/ADMIN" OR "myapp/EDITOR"
		// Both exist, should return true on first match
		final List<String> roles = Arrays.asList("ADMIN", "EDITOR");
		assertTrue(authFilter.checkRight(requestContext, securityContext, roles));
	}

	// ========================================================================
	// Test applicationName prefix behavior
	// ========================================================================

	@Test
	public void testCheckRight_ApplicationNamePrefix() throws SystemException {
		// The default implementation ALWAYS prefixes with applicationName
		// So @RolesAllowed("ADMIN") becomes "myapp/ADMIN"

		// User has "myapp/ADMIN" → should match
		final List<String> roles = Arrays.asList("ADMIN");
		assertTrue(authFilter.checkRight(requestContext, securityContext, roles));
	}

	@Test
	public void testCheckRight_ApplicationNamePrefix_ExplicitGroup_DoesNotWork() throws SystemException {
		// IMPORTANT: The default implementation prefixes ALL roles with applicationName
		// So @RolesAllowed("myapp/ADMIN") becomes "myapp/myapp/ADMIN"
		// MySecurityContext.isUserInRole("myapp/myapp/ADMIN") will parse as:
		//   - group = "myapp/myapp"  (everything before last /)
		//   - role = "ADMIN"
		// But user.right structure uses "myapp" as the group, not "myapp/myapp"

		// This means you SHOULD NOT use explicit application prefix in @RolesAllowed
		// when using default AuthenticationFilter - it will double-prefix!

		// User has rights in "myapp" group, not "myapp/myapp"
		// @RolesAllowed("myapp/ADMIN") → checks "myapp/myapp/ADMIN"
		final List<String> roles = Arrays.asList("myapp/ADMIN");
		assertFalse(authFilter.checkRight(requestContext, securityContext, roles));
	}

	// ========================================================================
	// Test with different application names
	// ========================================================================

	@Test
	public void testCheckRight_DifferentApplicationName() throws SystemException {
		// Create filter with different app name
		final TestableAuthenticationFilter otherAuthFilter = new TestableAuthenticationFilter("otherapp");

		// User has rights for "myapp", not "otherapp"
		// @RolesAllowed("ADMIN") → checks "otherapp/ADMIN"
		final List<String> roles = Arrays.asList("ADMIN");
		assertFalse(otherAuthFilter.checkRight(requestContext, securityContext, roles));
	}

	@Test
	public void testCheckRight_DifferentApplicationName_WithRights() throws SystemException {
		// Add rights for "otherapp"
		final Map<String, PartRight> otherAppRights = new HashMap<>();
		otherAppRights.put("ADMIN", PartRight.READ_WRITE);
		user.getRight().put("otherapp", otherAppRights);

		final TestableAuthenticationFilter otherAuthFilter = new TestableAuthenticationFilter("otherapp");

		// @RolesAllowed("ADMIN") → checks "otherapp/ADMIN"
		final List<String> roles = Arrays.asList("ADMIN");
		assertTrue(otherAuthFilter.checkRight(requestContext, securityContext, roles));
	}

	// ========================================================================
	// Test edge cases
	// ========================================================================

	@Test
	public void testCheckRight_EmptyRoleList() throws SystemException {
		// Edge case: no roles specified
		final List<String> roles = Arrays.asList();
		assertFalse(authFilter.checkRight(requestContext, securityContext, roles));
	}

	@Test
	public void testCheckRight_SpecialUserRole() throws SystemException {
		// Special role "USER" should work regardless of application prefix
		// "USER" → "myapp/USER" but MySecurityContext handles "USER" specially
		final List<String> roles = Arrays.asList("USER");

		// The default checkRight adds "myapp/" prefix, making it "myapp/USER"
		// MySecurityContext.isUserInRole("myapp/USER") will check group "myapp" for role "USER"
		// Since role "USER" is special in checkRightInGroup, it returns true if group exists
		assertTrue(authFilter.checkRight(requestContext, securityContext, roles));
	}

	@Test
	public void testCheckRight_UserWithNoRights() throws SystemException {
		// Create user with no rights
		final UserByToken emptyUser = new UserByToken();
		emptyUser.setOid(new ObjectId());
		emptyUser.setName("emptyuser");
		emptyUser.setType(UserByToken.TYPE_USER);
		emptyUser.setRight(new HashMap<>());

		final MySecurityContext emptyContext = new MySecurityContext(emptyUser, "https");

		// @RolesAllowed("ADMIN") → checks "myapp/ADMIN"
		final List<String> roles = Arrays.asList("ADMIN");
		assertFalse(authFilter.checkRight(requestContext, emptyContext, roles));
	}
}
