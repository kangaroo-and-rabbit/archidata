package test.atriasoft.archidata.model;

import java.util.HashMap;
import java.util.Map;

import org.atriasoft.archidata.model.token.JwtPayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestJwtPayload {

	private static JwtPayload payload(final Map<String, Map<String, Long>> roles) {
		return new JwtPayload("sub", "app", "iss", roles, null, "login", 2L, 1L);
	}

	@Test
	void theRolesCannotBeRaisedThroughTheMapGiven() {
		final Map<String, Long> group = new HashMap<>();
		group.put("ROLE", 1L);
		final Map<String, Map<String, Long>> roles = new HashMap<>();
		roles.put("app", group);
		final JwtPayload payload = payload(roles);

		group.put("ROLE", 3L);
		roles.put("other", Map.of("ROLE", 3L));

		Assertions.assertEquals(Map.of("app", Map.of("ROLE", 1L)), payload.roles());
	}

	@Test
	void theRolesCannotBeRaisedThroughTheAccessor() {
		final JwtPayload payload = payload(Map.of("app", Map.of("ROLE", 1L)));
		Assertions.assertThrows(UnsupportedOperationException.class,
				() -> payload.roles().put("other", Map.of("ROLE", 3L)));
		Assertions.assertThrows(UnsupportedOperationException.class, () -> payload.roles().get("app").put("ROLE", 3L));
	}

	@Test
	void anAbsentMapStaysNull() {
		Assertions.assertNull(payload(null).roles());
		Assertions.assertNull(payload(null).right());
	}
}
