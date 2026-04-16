package org.atriasoft.archidata.tools;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.filter.PartRight;

/**
 * Utility for generating fine-grained rights from a role-to-rights mapping.
 *
 * <p>
 * Given a mapping of role names to their associated right definitions, this class
 * computes the effective rights for a user based on their roles. When multiple roles
 * grant the same right, access levels are merged using bitmask OR logic.
 * </p>
 *
 * <p>Example usage:</p>
 * <pre>
 * RightRoleMapper mapper = new RightRoleMapper(Map.of(
 *     "ADMIN", List.of(
 *         new RightRoleMapper.RightDefinition("articles", PartRight.READ_WRITE),
 *         new RightRoleMapper.RightDefinition("users", PartRight.READ_WRITE)
 *     ),
 *     "USER", List.of(
 *         new RightRoleMapper.RightDefinition("articles", PartRight.READ),
 *         new RightRoleMapper.RightDefinition("users", PartRight.READ)
 *     )
 * ));
 *
 * Map&lt;String, PartRight&gt; rights = mapper.generateRights(
 *     Map.of("ADMIN", PartRight.READ_WRITE, "USER", PartRight.READ_WRITE)
 * );
 * // Result: {articles: READ_WRITE, users: READ_WRITE}
 * </pre>
 */
public class RightRoleMapper {

	/**
	 * Defines a single right with its name and access level.
	 *
	 * @param right the right name (e.g., "articles", "users")
	 * @param access the access level granted by this definition
	 */
	public record RightDefinition(
			String right,
			PartRight access) {}

	private final Map<String, List<RightDefinition>> roleToRights;

	/**
	 * Constructs a mapper with the given role-to-rights mapping.
	 *
	 * @param roleToRights mapping from role names to lists of right definitions
	 */
	public RightRoleMapper(final Map<String, List<RightDefinition>> roleToRights) {
		this.roleToRights = roleToRights;
	}

	/**
	 * Generates fine-grained rights from the user's roles.
	 *
	 * <p>
	 * For each role the user has, the associated right definitions are looked up.
	 * If multiple roles grant the same right, the access levels are merged using
	 * bitmask OR (e.g., READ | WRITE = READ_WRITE).
	 * Roles not present in the mapping are ignored.
	 * </p>
	 *
	 * @param userRoles the user's roles as a map of role name to access level
	 * @return the computed rights map
	 */
	public Map<String, PartRight> generateRights(final Map<String, PartRight> userRoles) {
		final Map<String, Integer> mergedRights = new HashMap<>();
		for (final Map.Entry<String, PartRight> roleEntry : userRoles.entrySet()) {
			final String roleName = roleEntry.getKey();
			final List<RightDefinition> definitions = this.roleToRights.get(roleName);
			if (definitions == null) {
				continue;
			}
			for (final RightDefinition definition : definitions) {
				final int current = mergedRights.getOrDefault(definition.right(), 0);
				mergedRights.put(definition.right(), current | definition.access().getValue());
			}
		}
		final Map<String, PartRight> result = new HashMap<>();
		for (final Map.Entry<String, Integer> entry : mergedRights.entrySet()) {
			result.put(entry.getKey(), PartRight.fromValue(entry.getValue()));
		}
		return result;
	}
}
