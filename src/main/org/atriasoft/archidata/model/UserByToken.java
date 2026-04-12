package org.atriasoft.archidata.model;

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
	 * @return the roles map
	 */
	public Map<String, Map<String, PartRight>> getRoles() {
		return this.roles;
	}

	/**
	 * Sets the roles map.
	 * @param roles the roles map to set
	 */
	public void setRoles(final Map<String, Map<String, PartRight>> roles) {
		this.roles = roles;
	}

	/**
	 * Gets the fine-grained rights map. The outer map key is the group name, the inner map key is the right name.
	 * @return the rights map
	 */
	public Map<String, Map<String, PartRight>> getRight() {
		return this.right;
	}

	/**
	 * Sets the fine-grained rights map.
	 * @param right the rights map to set
	 */
	public void setRight(final Map<String, Map<String, PartRight>> right) {
		this.right = right;
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
	 * Checks whether the entity has a specific role value for a given group and key.
	 * Supports Boolean, String, Long, and Double value types.
	 * @param group the group name
	 * @param key the role key within the group
	 * @param value the expected value to compare against
	 * @return {@code true} if the role exists and matches the given value, {@code false} otherwise
	 */
	public boolean hasRight(final String group, final String key, final Object value) {
		final Object data = getRoleForKey(group, key);
		if (data == null) {
			return false;
		}
		if (data instanceof final Boolean elem) {
			if (value instanceof final Boolean castVal) {
				if (elem.equals(castVal)) {
					return true;
				}
			}
			return false;
		}
		if (data instanceof final String elem) {
			if (value instanceof final String castVal) {
				if (elem.equals(castVal)) {
					return true;
				}
			}
			return false;
		}
		if (data instanceof final Long elem) {
			if (value instanceof final Long castVal) {
				if (elem.equals(castVal)) {
					return true;
				}
			}
			return false;
		}
		if (data instanceof final Double elem) {
			if (value instanceof final Double castVal) {
				if (elem.equals(castVal)) {
					return true;
				}
			}
			return false;
		}
		return false;
	}

}
