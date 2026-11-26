package org.atriasoft.archidata.tools;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.filter.PartRight;

/**
 * Utility for generating fine-grained rights from a role-to-rights mapping.
 *
 * <p>
 * Given a mapping of role names to their associated right definitions, this class
 * computes the effective rights for a user based on their roles. The level of each role
 * of the user caps the rights it gives (bitmask AND). When multiple roles
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
	 * @param roleToRights mapping from role names to lists of right definitions, copied
	 */
	public RightRoleMapper(final Map<String, List<RightDefinition>> roleToRights) {
		final Map<String, List<RightDefinition>> copy = new HashMap<>();
		for (final Map.Entry<String, List<RightDefinition>> entry : roleToRights.entrySet()) {
			copy.put(entry.getKey(), List.copyOf(entry.getValue()));
		}
		this.roleToRights = Collections.unmodifiableMap(copy);
	}

	/**
	 * Generates fine-grained rights from the user's roles.
	 *
	 * <p>
	 * For each role the user has, the associated right definitions are looked up.
	 * The level of the role caps what it gives, as in
	 * {@link org.atriasoft.archidata.filter.MySecurityContext}: the access level of each
	 * definition is combined with the level of the role by bitmask AND. A role at
	 * {@link PartRight#NONE} (or with a {@code null} level) gives nothing, a role at
	 * {@link PartRight#READ} gives only the read part of its rights, a role at
	 * {@link PartRight#WRITE} only the write part, a role at {@link PartRight#READ_WRITE} all of them.
	 * A right left with no access by this cap is not added.
	 * If multiple roles grant the same right, the capped access levels are merged using
	 * bitmask OR (e.g., READ | WRITE = READ_WRITE).
	 * Roles not present in the mapping are ignored.
	 * </p>
	 *
	 * @param userRoles the user's roles as a map of role name to the level of the role
	 * @return the computed rights map
	 */
	public Map<String, PartRight> generateRights(final Map<String, PartRight> userRoles) {
		final Map<String, Integer> mergedRights = new HashMap<>();
		for (final Map.Entry<String, PartRight> roleEntry : userRoles.entrySet()) {
			final String roleName = roleEntry.getKey();
			final PartRight roleLevel = roleEntry.getValue();
			if (roleLevel == null || roleLevel == PartRight.NONE) {
				continue;
			}
			final List<RightDefinition> definitions = this.roleToRights.get(roleName);
			if (definitions == null) {
				continue;
			}
			for (final RightDefinition definition : definitions) {
				final int capped = definition.access().getValue() & roleLevel.getValue();
				if (capped == 0 && definition.access() != PartRight.NONE) {
					continue;
				}
				final int current = mergedRights.getOrDefault(definition.right(), 0);
				mergedRights.put(definition.right(), current | capped);
			}
		}
		final Map<String, PartRight> result = new HashMap<>();
		for (final Map.Entry<String, Integer> entry : mergedRights.entrySet()) {
			result.put(entry.getKey(), PartRight.fromValue(entry.getValue()));
		}
		return result;
	}
}
