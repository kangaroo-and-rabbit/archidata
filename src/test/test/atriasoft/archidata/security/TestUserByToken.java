package test.atriasoft.archidata.security;

import java.util.HashMap;
import java.util.Map;

import org.atriasoft.archidata.filter.PartRight;
import org.atriasoft.archidata.model.UserByToken;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link UserByToken#hasRight} always answered {@code false}: the role is a {@link PartRight}, the method compared it
 * to {@code Boolean}, {@code String}, {@code Long} and {@code Double}. The roles and rights maps are also protected
 * here, except {@link UserByToken#getRoles()} that the applications write to.
 */
public class TestUserByToken {
	private static final String APP = "app";

	private UserByToken user;

	@BeforeEach
	void setUp() {
		this.user = new UserByToken();
		this.user.setRoles(Map.of(APP, Map.of(//
				"NONE_ROLE", PartRight.NONE, //
				"READ_ROLE", PartRight.READ, //
				"WRITE_ROLE", PartRight.WRITE, //
				"FULL_ROLE", PartRight.READ_WRITE)));
	}

	@Test
	void hasRightFollowsTheBitsOfThePartRight() {
		Assertions.assertTrue(this.user.hasRight(APP, "READ_ROLE", PartRight.READ));
		Assertions.assertTrue(this.user.hasRight(APP, "WRITE_ROLE", PartRight.WRITE));
		Assertions.assertTrue(this.user.hasRight(APP, "FULL_ROLE", PartRight.READ));
		Assertions.assertTrue(this.user.hasRight(APP, "FULL_ROLE", PartRight.WRITE));
		Assertions.assertTrue(this.user.hasRight(APP, "FULL_ROLE", PartRight.READ_WRITE));
	}

	@Test
	void hasRightNeverGrantsMoreThanTheRole() {
		Assertions.assertFalse(this.user.hasRight(APP, "READ_ROLE", PartRight.WRITE));
		Assertions.assertFalse(this.user.hasRight(APP, "READ_ROLE", PartRight.READ_WRITE));
		Assertions.assertFalse(this.user.hasRight(APP, "WRITE_ROLE", PartRight.READ));
		Assertions.assertFalse(this.user.hasRight(APP, "WRITE_ROLE", PartRight.READ_WRITE));
		Assertions.assertFalse(this.user.hasRight(APP, "NONE_ROLE", PartRight.READ));
		Assertions.assertFalse(this.user.hasRight(APP, "NONE_ROLE", PartRight.WRITE));
		Assertions.assertFalse(this.user.hasRight(APP, "NONE_ROLE", PartRight.READ_WRITE));
	}

	@Test
	void noneMatchesOnlyARoleOfNone() {
		Assertions.assertTrue(this.user.hasRight(APP, "NONE_ROLE", PartRight.NONE));
		// (right & 0) == 0 is true for every role: NONE must not become a way to pass the check.
		Assertions.assertFalse(this.user.hasRight(APP, "READ_ROLE", PartRight.NONE));
		Assertions.assertFalse(this.user.hasRight(APP, "FULL_ROLE", PartRight.NONE));
	}

	@Test
	void hasRightOfAMissingRoleIsFalse() {
		Assertions.assertFalse(this.user.hasRight(APP, "UNKNOWN", PartRight.NONE));
		Assertions.assertFalse(this.user.hasRight(APP, "UNKNOWN", false));
		Assertions.assertFalse(this.user.hasRight("other", "READ_ROLE", PartRight.READ));
	}

	@Test
	void hasRightReadsTheValueTypesOfItsContract() {
		Assertions.assertTrue(this.user.hasRight(APP, "READ_ROLE", "READ"));
		Assertions.assertTrue(this.user.hasRight(APP, "READ_ROLE", "read"));
		Assertions.assertFalse(this.user.hasRight(APP, "READ_ROLE", "WRITE"));
		Assertions.assertTrue(this.user.hasRight(APP, "READ_ROLE", 1L));
		Assertions.assertTrue(this.user.hasRight(APP, "READ_ROLE", 1));
		Assertions.assertTrue(this.user.hasRight(APP, "READ_ROLE", 1.0d));
		Assertions.assertFalse(this.user.hasRight(APP, "READ_ROLE", 2L));
		Assertions.assertFalse(this.user.hasRight(APP, "READ_ROLE", 3));
		Assertions.assertFalse(this.user.hasRight(APP, "READ_ROLE", 2.0d));
	}

	@Test
	void aBooleanAsksForTheFullRole() {
		Assertions.assertTrue(this.user.hasRight(APP, "FULL_ROLE", true));
		Assertions.assertFalse(this.user.hasRight(APP, "READ_ROLE", true));
		Assertions.assertFalse(this.user.hasRight(APP, "WRITE_ROLE", true));
		Assertions.assertFalse(this.user.hasRight(APP, "NONE_ROLE", true));
		Assertions.assertTrue(this.user.hasRight(APP, "NONE_ROLE", false));
		Assertions.assertFalse(this.user.hasRight(APP, "READ_ROLE", false));
	}

	@Test
	void aValueThatDescribesNoRightGrantsNothing() {
		for (final Object value : new Object[] { null, "ADMIN", "", 4L, -1, 1.5d, Double.NaN, Double.POSITIVE_INFINITY,
				new Object() }) {
			Assertions.assertFalse(this.user.hasRight(APP, "FULL_ROLE", value), () -> "value=" + value);
			Assertions.assertFalse(this.user.hasRight(APP, "NONE_ROLE", value), () -> "value=" + value);
		}
	}

	@Test
	void getRolesStaysWritable() {
		// KarsoAuthenticationFilter and KarideoAuthenticationFilter build the token of an application key this way.
		final UserByToken application = new UserByToken();
		application.getRoles().put(APP, Map.of("APPLICATION", PartRight.READ_WRITE));
		Assertions.assertEquals(PartRight.READ_WRITE, application.getRoleForKey(APP, "APPLICATION"));
		this.user.getRoles().put("other", Map.of("ROLE", PartRight.READ));
		Assertions.assertEquals(PartRight.READ, this.user.getRoleForKey("other", "ROLE"));
	}

	@Test
	void theRolesGivenToTheSetterAreCopied() {
		final Map<String, PartRight> group = new HashMap<>();
		group.put("ROLE", PartRight.READ);
		final Map<String, Map<String, PartRight>> roles = new HashMap<>();
		roles.put(APP, group);
		this.user.setRoles(roles);

		group.put("ROLE", PartRight.READ_WRITE);
		roles.put("other", Map.of("ROLE", PartRight.READ_WRITE));

		Assertions.assertEquals(PartRight.READ, this.user.getRoleForKey(APP, "ROLE"));
		Assertions.assertFalse(this.user.groupExist("other"));
	}

	@Test
	void theRightsGivenToTheSetterAreCopied() {
		final Map<String, PartRight> group = new HashMap<>();
		group.put("articles", PartRight.READ);
		final Map<String, Map<String, PartRight>> right = new HashMap<>();
		right.put(APP, group);
		this.user.setRight(right);

		group.put("articles", PartRight.READ_WRITE);
		right.put("other", Map.of("articles", PartRight.READ_WRITE));

		Assertions.assertEquals(PartRight.READ, this.user.getRightForKey(APP, "articles"));
		Assertions.assertNull(this.user.getRightForKey("other", "articles"));
	}

	@Test
	void theRightsCannotBeRaisedThroughTheGetter() {
		this.user.setRight(Map.of(APP, Map.of("articles", PartRight.READ)));
		final Map<String, Map<String, PartRight>> right = this.user.getRight();
		Assertions.assertEquals(Map.of(APP, Map.of("articles", PartRight.READ)), right);
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> right.put("other", Map.of("articles", PartRight.READ_WRITE)));
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> right.get(APP).put("articles", PartRight.READ_WRITE));
		Assertions.assertEquals(PartRight.READ, this.user.getRightForKey(APP, "articles"));
	}

	@Test
	void aNullMapStaysNull() {
		this.user.setRoles(null);
		this.user.setRight(null);
		Assertions.assertNull(this.user.getRoles());
		Assertions.assertNull(this.user.getRight());
	}
}
