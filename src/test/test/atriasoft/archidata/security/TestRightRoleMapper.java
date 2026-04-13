package test.atriasoft.archidata.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.filter.PartRight;
import org.atriasoft.archidata.tools.RightRoleMapper;
import org.atriasoft.archidata.tools.RightRoleMapper.RightDefinition;
import org.junit.jupiter.api.Test;

/**
 * Test suite for {@link RightRoleMapper}.
 *
 * <p>
 * Verifies that rights are correctly generated from role mappings,
 * including bitmask OR fusion when multiple roles grant the same right.
 * </p>
 */
public class TestRightRoleMapper {

	// ========================================================================
	// Test single role mapping
	// ========================================================================

	@Test
	public void testSingleRole_SingleRight() {
		final RightRoleMapper mapper = new RightRoleMapper(
				Map.of("ADMIN", List.of(new RightDefinition("articles", PartRight.READ_WRITE))));

		final Map<String, PartRight> result = mapper.generateRights(Map.of("ADMIN", PartRight.READ_WRITE));

		assertEquals(1, result.size());
		assertEquals(PartRight.READ_WRITE, result.get("articles"));
	}

	@Test
	public void testSingleRole_MultipleRights() {
		final RightRoleMapper mapper = new RightRoleMapper(Map.of("ADMIN",
				List.of(new RightDefinition("articles", PartRight.READ_WRITE),
						new RightDefinition("users", PartRight.READ_WRITE),
						new RightDefinition("logs", PartRight.READ))));

		final Map<String, PartRight> result = mapper.generateRights(Map.of("ADMIN", PartRight.READ_WRITE));

		assertEquals(3, result.size());
		assertEquals(PartRight.READ_WRITE, result.get("articles"));
		assertEquals(PartRight.READ_WRITE, result.get("users"));
		assertEquals(PartRight.READ, result.get("logs"));
	}

	// ========================================================================
	// Test multiple roles — bitmask OR fusion
	// ========================================================================

	@Test
	public void testMultipleRoles_MergeBitmask() {
		// ADMIN gives articles:READ, USER gives articles:WRITE
		// Merged: articles:READ_WRITE (1 | 2 == 3)
		final RightRoleMapper mapper = new RightRoleMapper(
				Map.of("ADMIN", List.of(new RightDefinition("articles", PartRight.READ)), "USER",
						List.of(new RightDefinition("articles", PartRight.WRITE))));

		final Map<String, PartRight> result = mapper
				.generateRights(Map.of("ADMIN", PartRight.READ_WRITE, "USER", PartRight.READ_WRITE));

		assertEquals(1, result.size());
		assertEquals(PartRight.READ_WRITE, result.get("articles"));
	}

	@Test
	public void testMultipleRoles_DifferentRights() {
		// ADMIN gives articles:RW, USER gives users:READ
		final RightRoleMapper mapper = new RightRoleMapper(
				Map.of("ADMIN", List.of(new RightDefinition("articles", PartRight.READ_WRITE)), "USER",
						List.of(new RightDefinition("users", PartRight.READ))));

		final Map<String, PartRight> result = mapper
				.generateRights(Map.of("ADMIN", PartRight.READ_WRITE, "USER", PartRight.READ_WRITE));

		assertEquals(2, result.size());
		assertEquals(PartRight.READ_WRITE, result.get("articles"));
		assertEquals(PartRight.READ, result.get("users"));
	}

	@Test
	public void testMultipleRoles_SameRightSameAccess() {
		// Both roles give articles:READ → merged: articles:READ (1 | 1 == 1)
		final RightRoleMapper mapper = new RightRoleMapper(
				Map.of("ADMIN", List.of(new RightDefinition("articles", PartRight.READ)), "USER",
						List.of(new RightDefinition("articles", PartRight.READ))));

		final Map<String, PartRight> result = mapper
				.generateRights(Map.of("ADMIN", PartRight.READ_WRITE, "USER", PartRight.READ_WRITE));

		assertEquals(1, result.size());
		assertEquals(PartRight.READ, result.get("articles"));
	}

	// ========================================================================
	// Test unknown roles — ignored
	// ========================================================================

	@Test
	public void testUnknownRole_Ignored() {
		final RightRoleMapper mapper = new RightRoleMapper(
				Map.of("ADMIN", List.of(new RightDefinition("articles", PartRight.READ_WRITE))));

		// User has role "UNKNOWN" which is not in the mapping
		final Map<String, PartRight> result = mapper.generateRights(Map.of("UNKNOWN", PartRight.READ_WRITE));

		assertTrue(result.isEmpty());
	}

	@Test
	public void testPartialRoleMatch() {
		final RightRoleMapper mapper = new RightRoleMapper(
				Map.of("ADMIN", List.of(new RightDefinition("articles", PartRight.READ_WRITE)), "USER",
						List.of(new RightDefinition("users", PartRight.READ))));

		// User only has USER role, not ADMIN
		final Map<String, PartRight> result = mapper.generateRights(Map.of("USER", PartRight.READ_WRITE));

		assertEquals(1, result.size());
		assertEquals(PartRight.READ, result.get("users"));
	}

	// ========================================================================
	// Test empty inputs
	// ========================================================================

	@Test
	public void testEmptyRoles_EmptyResult() {
		final RightRoleMapper mapper = new RightRoleMapper(
				Map.of("ADMIN", List.of(new RightDefinition("articles", PartRight.READ_WRITE))));

		final Map<String, PartRight> result = mapper.generateRights(new HashMap<>());

		assertTrue(result.isEmpty());
	}

	@Test
	public void testEmptyMapping_EmptyResult() {
		final RightRoleMapper mapper = new RightRoleMapper(new HashMap<>());

		final Map<String, PartRight> result = mapper.generateRights(Map.of("ADMIN", PartRight.READ_WRITE));

		assertTrue(result.isEmpty());
	}

	// ========================================================================
	// Test realistic scenario
	// ========================================================================

	@Test
	public void testRealisticScenario() {
		// Configuration like a real app:
		// ADMIN → articles:RW, users:RW, settings:RW
		// EDITOR → articles:RW, users:R
		// USER → articles:R, users:R
		final RightRoleMapper mapper = new RightRoleMapper(Map.of("ADMIN",
				List.of(new RightDefinition("articles", PartRight.READ_WRITE),
						new RightDefinition("users", PartRight.READ_WRITE),
						new RightDefinition("settings", PartRight.READ_WRITE)),
				"EDITOR",
				List.of(new RightDefinition("articles", PartRight.READ_WRITE),
						new RightDefinition("users", PartRight.READ)),
				"USER", List.of(new RightDefinition("articles", PartRight.READ),
						new RightDefinition("users", PartRight.READ))));

		// User with EDITOR + USER roles
		final Map<String, PartRight> result = mapper
				.generateRights(Map.of("EDITOR", PartRight.READ_WRITE, "USER", PartRight.READ_WRITE));

		assertEquals(2, result.size());
		// articles: RW (from EDITOR) | R (from USER) = RW
		assertEquals(PartRight.READ_WRITE, result.get("articles"));
		// users: R (from EDITOR) | R (from USER) = R
		assertEquals(PartRight.READ, result.get("users"));
		// settings: not present (only ADMIN has it)
	}
}
