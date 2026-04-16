package test.atriasoft.archidata.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.atriasoft.archidata.filter.MySecurityContext;
import org.atriasoft.archidata.filter.PartRight;
import org.atriasoft.archidata.model.UserByToken;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test suite for {@link MySecurityContext#isUserInRole(String)} method.
 *
 * This class verifies the role checking logic for SSO/JWT authentication in archidata.
 *
 * <h2>Role Format Patterns</h2>
 * <ul>
 * <li><b>"ROLE"</b> → checks in "?system?" group</li>
 * <li><b>"ROLE:r"</b> → checks READ permission</li>
 * <li><b>"ROLE:w"</b> → checks WRITE permission</li>
 * <li><b>"ROLE:rw"</b> → checks READ_WRITE permission</li>
 * <li><b>"group/ROLE"</b> → checks ROLE in specific group</li>
 * <li><b>"group/ROLE:r"</b> → checks READ in specific group</li>
 * <li><b>"group/ROLE:w"</b> → checks WRITE in specific group</li>
 * <li><b>"USER"</b> → always true if token is valid</li>
 * </ul>
 *
 * <h2>Right Structure in JWT</h2>
 *
 * <pre>
 * {
 *   "right": {
 *     "myapp": {
 *       "ADMIN": "READ_WRITE",
 *       "EDITOR": "WRITE",
 *       "VIEWER": "READ"
 *     },
 *     "?system?": {
 *       "SUPER_ADMIN": "READ_WRITE"
 *     }
 *   }
 * }
 * </pre>
 */
public class TestMySecurityContext {

	private MySecurityContext securityContext;
	private UserByToken user;

	@BeforeEach
	public void setup() {
		user = new UserByToken();
		user.setOid(new ObjectId());
		user.setName("testuser");
		user.setType(UserByToken.TYPE_USER);

		// Setup rights structure:
		// - myapp/ADMIN: READ_WRITE
		// - myapp/EDITOR: WRITE
		// - myapp/VIEWER: READ
		// - ?system?/SUPER_ADMIN: READ_WRITE
		final Map<String, Map<String, PartRight>> rights = new HashMap<>();

		final Map<String, PartRight> myappRights = new HashMap<>();
		myappRights.put("ADMIN", PartRight.READ_WRITE);
		myappRights.put("EDITOR", PartRight.WRITE);
		myappRights.put("VIEWER", PartRight.READ);
		rights.put("myapp", myappRights);

		final Map<String, PartRight> systemRights = new HashMap<>();
		systemRights.put("SUPER_ADMIN", PartRight.READ_WRITE);
		rights.put("?system?", systemRights);

		user.setRoles(rights);

		securityContext = new MySecurityContext(user, "https");
	}

	// ========================================================================
	// Test special "USER" role
	// ========================================================================

	@Test
	public void testUserRole_AlwaysTrue() {
		// "USER" role should always return true if token is valid
		assertTrue(securityContext.isUserInRole("USER"));
	}

	// ========================================================================
	// Test system group (no prefix)
	// ========================================================================

	@Test
	public void testSystemRole_WithReadWrite() {
		// User has ?system?/SUPER_ADMIN with READ_WRITE
		assertTrue(securityContext.isUserInRole("SUPER_ADMIN"));
	}

	@Test
	public void testSystemRole_WithReadWrite_ExplicitRead() {
		// User has ?system?/SUPER_ADMIN with READ_WRITE, should allow :r
		assertTrue(securityContext.isUserInRole("SUPER_ADMIN:r"));
	}

	@Test
	public void testSystemRole_WithReadWrite_ExplicitWrite() {
		// User has ?system?/SUPER_ADMIN with READ_WRITE, should allow :w
		assertTrue(securityContext.isUserInRole("SUPER_ADMIN:w"));
	}

	@Test
	public void testSystemRole_WithReadWrite_ExplicitReadWrite() {
		// User has ?system?/SUPER_ADMIN with READ_WRITE, should allow :rw
		assertTrue(securityContext.isUserInRole("SUPER_ADMIN:rw"));
	}

	@Test
	public void testSystemRole_NotExist() {
		// User does not have UNKNOWN_ROLE in ?system?
		assertFalse(securityContext.isUserInRole("UNKNOWN_ROLE"));
	}

	// ========================================================================
	// Test group-prefixed roles (group/ROLE)
	// ========================================================================

	@Test
	public void testGroupRole_WithReadWrite() {
		// User has myapp/ADMIN with READ_WRITE
		assertTrue(securityContext.isUserInRole("myapp/ADMIN"));
	}

	@Test
	public void testGroupRole_WithReadWrite_ExplicitRead() {
		// User has myapp/ADMIN with READ_WRITE, should allow :r
		assertTrue(securityContext.isUserInRole("myapp/ADMIN:r"));
	}

	@Test
	public void testGroupRole_WithReadWrite_ExplicitWrite() {
		// User has myapp/ADMIN with READ_WRITE, should allow :w
		assertTrue(securityContext.isUserInRole("myapp/ADMIN:w"));
	}

	@Test
	public void testGroupRole_WithReadWrite_ExplicitReadWrite() {
		// User has myapp/ADMIN with READ_WRITE, should allow :rw
		assertTrue(securityContext.isUserInRole("myapp/ADMIN:rw"));
	}

	// ========================================================================
	// Test READ-only roles
	// ========================================================================

	@Test
	public void testGroupRole_WithReadOnly() {
		// User has myapp/VIEWER with READ, no suffix should fail
		assertFalse(securityContext.isUserInRole("myapp/VIEWER"));
	}

	@Test
	public void testGroupRole_WithReadOnly_ExplicitRead() {
		// User has myapp/VIEWER with READ, should allow :r
		assertTrue(securityContext.isUserInRole("myapp/VIEWER:r"));
	}

	@Test
	public void testGroupRole_WithReadOnly_ExplicitWrite() {
		// User has myapp/VIEWER with READ, should NOT allow :w
		assertFalse(securityContext.isUserInRole("myapp/VIEWER:w"));
	}

	@Test
	public void testGroupRole_WithReadOnly_ExplicitReadWrite() {
		// User has myapp/VIEWER with READ, should NOT allow :rw
		assertFalse(securityContext.isUserInRole("myapp/VIEWER:rw"));
	}

	// ========================================================================
	// Test WRITE-only roles
	// ========================================================================

	@Test
	public void testGroupRole_WithWriteOnly() {
		// User has myapp/EDITOR with WRITE, no suffix should fail
		assertFalse(securityContext.isUserInRole("myapp/EDITOR"));
	}

	@Test
	public void testGroupRole_WithWriteOnly_ExplicitRead() {
		// User has myapp/EDITOR with WRITE, should NOT allow :r
		assertFalse(securityContext.isUserInRole("myapp/EDITOR:r"));
	}

	@Test
	public void testGroupRole_WithWriteOnly_ExplicitWrite() {
		// User has myapp/EDITOR with WRITE, should allow :w
		assertTrue(securityContext.isUserInRole("myapp/EDITOR:w"));
	}

	@Test
	public void testGroupRole_WithWriteOnly_ExplicitReadWrite() {
		// User has myapp/EDITOR with WRITE, should NOT allow :rw
		assertFalse(securityContext.isUserInRole("myapp/EDITOR:rw"));
	}

	// ========================================================================
	// Test non-existent groups and roles
	// ========================================================================

	@Test
	public void testGroupRole_GroupNotExist() {
		// User does not have rights in "unknown" group
		assertFalse(securityContext.isUserInRole("unknown/ADMIN"));
	}

	@Test
	public void testGroupRole_RoleNotExist() {
		// User does not have myapp/UNKNOWN_ROLE
		assertFalse(securityContext.isUserInRole("myapp/UNKNOWN_ROLE"));
	}

	// ========================================================================
	// Test special "USER" role in specific group
	// ========================================================================

	@Test
	public void testGroupUser_GroupExists() {
		// "myapp/USER" should return true if user has any right in "myapp"
		assertTrue(securityContext.isUserInRole("myapp/USER"));
	}

	@Test
	public void testGroupUser_GroupNotExists() {
		// "unknown/USER" should return false if user has no rights in "unknown"
		assertFalse(securityContext.isUserInRole("unknown/USER"));
	}

	// ========================================================================
	// Test edge cases
	// ========================================================================

	@Test
	public void testEmptyUser() {
		// Create empty user with no rights
		final UserByToken emptyUser = new UserByToken();
		emptyUser.setOid(new ObjectId());
		emptyUser.setName("emptyuser");
		emptyUser.setType(UserByToken.TYPE_USER);
		emptyUser.setRoles(new HashMap<>());

		final MySecurityContext emptyContext = new MySecurityContext(emptyUser, "https");

		// USER role should still work
		assertTrue(emptyContext.isUserInRole("USER"));

		// Everything else should fail
		assertFalse(emptyContext.isUserInRole("ADMIN"));
		assertFalse(emptyContext.isUserInRole("myapp/ADMIN"));
		assertFalse(emptyContext.isUserInRole("SUPER_ADMIN"));
	}
}
