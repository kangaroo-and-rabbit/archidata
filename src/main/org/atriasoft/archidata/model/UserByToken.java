package org.atriasoft.archidata.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.filter.PartRight;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a user or application identity extracted from a JWT token. Holds the entity type, identifiers, name, and associated access rights.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserByToken {
	/** Creates an empty {@link UserByToken} instance. */
	public UserByToken() {}

	/** Type constant indicating a regular user. */
	// Set here by number to permit to extend it by the user.
	public static final int TYPE_USER = -1;
	/** Type constant indicating an application. */
	public static final int TYPE_APPLICATION = -2;
	// application internal management type: an application generic Id
	private Integer type = null;

	private ObjectId oid = null;
	// For application, this is the id of the application, and of user token, this is the USERID
	private ObjectId parentId = null;
	private String name = null;
	// Roles map (high-level roles: ADMIN, USER, etc.)
	private Map<String, Map<String, PartRight>> roles = new HashMap<>();
	// Rights map (fine-grained resource rights: articles, users, etc.)
	private Map<String, Map<String, PartRight>> right = new HashMap<>();

	/**
	 * Gets the type of this token entity.
	 * @return the type identifier (e.g. {@link #TYPE_USER} or {@link #TYPE_APPLICATION})
	 */
	public Integer getType() {
		return this.type;
	}

	/**
	 * Sets the type of this token entity.
	 * @param type the type identifier to set
	 */
	public void setType(final Integer type) {
		this.type = type;
	}

	/**
	 * Gets the ObjectId of this token entity.
	 * @return the ObjectId
	 */
	public ObjectId getOid() {
		return this.oid;
	}

	/**
	 * Sets the ObjectId of this token entity.
	 * @param oid the ObjectId to set
	 */
	public void setOid(final ObjectId oid) {
		this.oid = oid;
	}

	/**
	 * Gets the parent entity identifier. For applications, this is the application ID; for user tokens, this is the user ID.
	 * @return the parent ObjectId
	 */
	public ObjectId getParentId() {
		return this.parentId;
	}

	/**
	 * Sets the parent entity identifier.
	 * @param parentId the parent ObjectId to set
	 */
	public void setParentId(final ObjectId parentId) {
		this.parentId = parentId;
	}

	/**
	 * Gets the name associated with this token entity.
	 * @return the name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Sets the name associated with this token entity.
	 * @param name the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Gets the roles map. The outer map key is the group name, the inner map key is the role name.
	 * <p>
	 * This is the live map, and it stays writable: applications add a role to the token of an application key with
	 * {@code getRoles().put(...)}. {@link #getRight()} is read-only.
	 * @return the roles map
	 */
	public Map<String, Map<String, PartRight>> getRoles() {
		return this.roles;
	}

	/**
	 * Sets the roles map. The map is copied: changing it afterwards does not change the roles of this entity.
	 * @param roles the roles map to set
	 */
	public void setRoles(final Map<String, Map<String, PartRight>> roles) {
		this.roles = copyRights(roles);
	}

	/**
	 * Gets the fine-grained rights map. The outer map key is the group name, the inner map key is the right name.
	 * @return a read-only view of the rights map: writing to it throws {@link UnsupportedOperationException}, use
	 *         {@link #setRight(Map)}
	 */
	public Map<String, Map<String, PartRight>> getRight() {
		if (this.right == null) {
			return null;
		}
		final Map<String, Map<String, PartRight>> out = new HashMap<>();
		for (final Map.Entry<String, Map<String, PartRight>> entry : this.right.entrySet()) {
			final Map<String, PartRight> rightGroup = entry.getValue();
			out.put(entry.getKey(), rightGroup == null ? null : Collections.unmodifiableMap(rightGroup));
		}
		return Collections.unmodifiableMap(out);
	}

	/**
	 * Sets the fine-grained rights map. The map is copied: changing it afterwards does not change the rights of this
	 * entity.
	 * @param right the rights map to set
	 */
	public void setRight(final Map<String, Map<String, PartRight>> right) {
		this.right = copyRights(right);
	}

	private static Map<String, Map<String, PartRight>> copyRights(final Map<String, Map<String, PartRight>> source) {
		if (source == null) {
			return null;
		}
		final Map<String, Map<String, PartRight>> out = new HashMap<>();
		for (final Map.Entry<String, Map<String, PartRight>> entry : source.entrySet()) {
			final Map<String, PartRight> group = entry.getValue();
			out.put(entry.getKey(), group == null ? null : new HashMap<>(group));
		}
		return out;
	}

	/**
	 * Gets the set of all group names that this entity has roles in.
	 * @return the set of group names
	 */
	public Set<String> getGroups() {
		return this.roles.keySet();
	}

	/**
	 * Checks whether a given group exists in the roles map.
	 * @param group the group name to check
	 * @return {@code true} if the group exists, {@code false} otherwise
	 */
	public boolean groupExist(final String group) {
		return this.roles.containsKey(group);
	}

	/**
	 * Retrieves the role value for a specific group and role name combination.
	 * @param group the group name
	 * @param key the role name within the group
	 * @return the {@link PartRight} value, or {@code null} if the group or key does not exist
	 */
	public PartRight getRoleForKey(final String group, final String key) {
		if (!this.roles.containsKey(group)) {
			return null;
		}
		final Map<String, PartRight> roleGroup = this.roles.get(group);
		if (!roleGroup.containsKey(key)) {
			return null;
		}
		return roleGroup.get(key);
	}

	/**
	 * Retrieves the fine-grained right value for a specific group and right name combination.
	 * @param group the group name
	 * @param key the right name within the group
	 * @return the {@link PartRight} value, or {@code null} if the group or key does not exist
	 */
	public PartRight getRightForKey(final String group, final String key) {
		if (!this.right.containsKey(group)) {
			return null;
		}
		final Map<String, PartRight> rightGroup = this.right.get(group);
		if (!rightGroup.containsKey(key)) {
			return null;
		}
		return rightGroup.get(key);
	}

	/**
	 * Checks whether the role {@code key} of a group gives the access described by {@code value}.
	 * <p>
	 * The role is a {@link PartRight} bitmask, so a role grants what its bits include, as
	 * {@link org.atriasoft.archidata.filter.MySecurityContext#hasResourceRight} and
	 * {@link org.atriasoft.archidata.filter.MySecurityContext#checkRightInGroup} do: {@code READ_WRITE} satisfies
	 * {@code READ} and {@code WRITE}; {@code READ} does not satisfy {@code WRITE}. {@code value} can be:
	 * <ul>
	 * <li>a {@link PartRight};</li>
	 * <li>a {@code String}, the name of a right ({@link PartRight#fromString(String)});</li>
	 * <li>a {@code Long}, an {@code Integer} or a whole {@code Double}, the code of a right
	 * ({@link PartRight#fromValue(long)});</li>
	 * <li>a {@code Boolean}: {@code true} asks for {@code READ_WRITE}, like a role without {@code :r} or {@code :w}
	 * suffix in {@link org.atriasoft.archidata.filter.MySecurityContext#isUserInRole(String)}, {@code false} asks for
	 * {@code NONE}.</li>
	 * </ul>
	 * {@code NONE} grants nothing, it does not match every role: it only matches a role whose value is {@code NONE}.
	 * Anything else ({@code null}, an unknown name or code, another type) grants nothing.
	 * @param group the group name
	 * @param key the role key within the group
	 * @param value the access to check
	 * @return {@code true} if the role exists and gives this access, {@code false} otherwise
	 */
	public boolean hasRight(final String group, final String key, final Object value) {
		final PartRight held = getRoleForKey(group, key);
		final PartRight asked = toPartRight(value);
		if (held == null || asked == null) {
			return false;
		}
		if (asked == PartRight.NONE) {
			return held == PartRight.NONE;
		}
		return (held.getValue() & asked.getValue()) == asked.getValue();
	}

	/** The {@link PartRight} described by a value, {@code null} when it describes none. */
	private static PartRight toPartRight(final Object value) {
		try {
			return switch (value) {
				case final PartRight elem -> elem;
				case final Boolean elem -> elem ? PartRight.READ_WRITE : PartRight.NONE;
				case final String elem -> PartRight.fromString(elem);
				case final Long elem -> PartRight.fromValue(elem.longValue());
				case final Integer elem -> PartRight.fromValue(elem.intValue());
				case final Double elem when elem == Math.rint(elem) -> PartRight.fromValue(elem.longValue());
				case null, default -> null;
			};
		} catch (final IllegalArgumentException ex) {
			return null;
		}
	}

}
