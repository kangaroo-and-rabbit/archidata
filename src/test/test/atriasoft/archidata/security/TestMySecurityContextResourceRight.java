package test.atriasoft.archidata.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
 * Test suite for {@link MySecurityContext#hasResourceRight(String, String, PartRight)}
 * and {@link MySecurityContext#getResourceRight(String, String)} methods.
 *
 * <p>
 * Verifies the programmatic fine-grained resource-level rights checking
 * with bitmask logic.
 * </p>
 */
public class TestMySecurityContextResourceRight {

	private MySecurityContext securityContext;
	private UserByToken user;

	@BeforeEach
	public void setup() {
		user = new UserByToken();
		user.setOid(new ObjectId());
		user.setName("testuser");
		user.setType(UserByToken.TYPE_USER);

		// Setup roles (high-level)
		final Map<String, Map<String, PartRight>> roles = new HashMap<>();
		final Map<String, PartRight> myappRoles = new HashMap<>();
		myappRoles.put("ADMIN", PartRight.READ_WRITE);
		roles.put("myapp", myappRoles);
		user.setRoles(roles);

		// Setup fine-grained rights
		final Map<String, Map<String, PartRight>> rights = new HashMap<>();
		final Map<String, PartRight> myappRights = new HashMap<>();
		myappRights.put("articles", PartRight.READ_WRITE);
		myappRights.put("users", PartRight.READ);
		myappRights.put("logs", PartRight.WRITE);
		myappRights.put("config", PartRight.NONE);
		rights.put("myapp", myappRights);
		user.setRight(rights);

		securityContext = new MySecurityContext(user, "https");
	}

	// ========================================================================
	// Test hasResourceRight — bitmask logic
	// ========================================================================

	@Test
	public void testHasResourceRight_ReadWrite_RequireRead() {
		assertTrue(securityContext.hasResourceRight("myapp", "articles", PartRight.READ));
	}

	@Test
	public void testHasResourceRight_ReadWrite_RequireWrite() {
		assertTrue(securityContext.hasResourceRight("myapp", "articles", PartRight.WRITE));
	}

	@Test
	public void testHasResourceRight_ReadWrite_RequireReadWrite() {
		assertTrue(securityContext.hasResourceRight("myapp", "articles", PartRight.READ_WRITE));
	}

	@Test
	public void testHasResourceRight_ReadOnly_RequireRead() {
		assertTrue(securityContext.hasResourceRight("myapp", "users", PartRight.READ));
	}

	@Test
	public void testHasResourceRight_ReadOnly_RequireWrite() {
		assertFalse(securityContext.hasResourceRight("myapp", "users", PartRight.WRITE));
	}

	@Test
	public void testHasResourceRight_ReadOnly_RequireReadWrite() {
		assertFalse(securityContext.hasResourceRight("myapp", "users", PartRight.READ_WRITE));
	}

	@Test
	public void testHasResourceRight_WriteOnly_RequireRead() {
		assertFalse(securityContext.hasResourceRight("myapp", "logs", PartRight.READ));
	}

	@Test
	public void testHasResourceRight_WriteOnly_RequireWrite() {
		assertTrue(securityContext.hasResourceRight("myapp", "logs", PartRight.WRITE));
	}

	@Test
	public void testHasResourceRight_WriteOnly_RequireReadWrite() {
		assertFalse(securityContext.hasResourceRight("myapp", "logs", PartRight.READ_WRITE));
	}

	@Test
	public void testHasResourceRight_None_RequireNone() {
		assertTrue(securityContext.hasResourceRight("myapp", "config", PartRight.NONE));
	}

	@Test
	public void testHasResourceRight_None_RequireRead() {
		assertFalse(securityContext.hasResourceRight("myapp", "config", PartRight.READ));
	}

	// ========================================================================
	// Test hasResourceRight — non-existent
	// ========================================================================

	@Test
	public void testHasResourceRight_NonExistentRight() {
		assertFalse(securityContext.hasResourceRight("myapp", "comments", PartRight.READ));
	}

	@Test
	public void testHasResourceRight_NonExistentApplication() {
		assertFalse(securityContext.hasResourceRight("otherapp", "articles", PartRight.READ));
	}

	// ========================================================================
	// Test getResourceRight
	// ========================================================================

	@Test
	public void testGetResourceRight_Exists() {
		assertEquals(PartRight.READ_WRITE, securityContext.getResourceRight("myapp", "articles"));
	}

	@Test
	public void testGetResourceRight_ReadOnly() {
		assertEquals(PartRight.READ, securityContext.getResourceRight("myapp", "users"));
	}

	@Test
	public void testGetResourceRight_WriteOnly() {
		assertEquals(PartRight.WRITE, securityContext.getResourceRight("myapp", "logs"));
	}

	@Test
	public void testGetResourceRight_None() {
		assertEquals(PartRight.NONE, securityContext.getResourceRight("myapp", "config"));
	}

	@Test
	public void testGetResourceRight_NonExistent() {
		assertNull(securityContext.getResourceRight("myapp", "comments"));
	}

	@Test
	public void testGetResourceRight_NonExistentApplication() {
		assertNull(securityContext.getResourceRight("otherapp", "articles"));
	}

	// ========================================================================
	// Test roles and rights independence
	// ========================================================================

	@Test
	public void testRolesAndRightsAreIndependent() {
		// hasResourceRight checks rights, not roles
		// User has role ADMIN but no right named "ADMIN"
		assertFalse(securityContext.hasResourceRight("myapp", "ADMIN", PartRight.READ));
		assertNull(securityContext.getResourceRight("myapp", "ADMIN"));

		// isUserInRole checks roles, not rights
		// User has right "articles" but no role named "articles"
		assertFalse(securityContext.isUserInRole("myapp/articles"));
	}

	// ========================================================================
	// Test with null user
	// ========================================================================

	@Test
	public void testHasResourceRight_NullUser() {
		final MySecurityContext nullContext = new MySecurityContext(null, "https");
		assertFalse(nullContext.hasResourceRight("myapp", "articles", PartRight.READ));
	}

	@Test
	public void testGetResourceRight_NullUser() {
		final MySecurityContext nullContext = new MySecurityContext(null, "https");
		assertNull(nullContext.getResourceRight("myapp", "articles"));
	}

	// ========================================================================
	// Test with empty rights
	// ========================================================================

	@Test
	public void testHasResourceRight_EmptyRights() {
		final UserByToken emptyUser = new UserByToken();
		emptyUser.setOid(new ObjectId());
		emptyUser.setName("emptyuser");
		emptyUser.setType(UserByToken.TYPE_USER);

		final MySecurityContext emptyContext = new MySecurityContext(emptyUser, "https");
		assertFalse(emptyContext.hasResourceRight("myapp", "articles", PartRight.READ));
	}
}
