package org.atriasoft.archidata.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.filter.PartRight;
import org.bson.types.ObjectId;

public class UserByToken {
	// Set here by number to permit to extend it by the user.
	public static final int TYPE_USER = -1;
	public static final int TYPE_APPLICATION = -2;
	// application internal management type: an application generic Id
	public Integer type = null;

	public ObjectId oid = null;
	// For application, this is the id of the application, and of user token, this is the USERID
	public ObjectId parentId = null;
	public String name = null;
	// Right map
	public Map<String, Map<String, PartRight>> right = new HashMap<>();

	public Set<String> getGroups() {
		return this.right.keySet();
	}

	public boolean groupExist(final String group) {
		if (!this.right.containsKey(group)) {
			return false;
		}
		return this.right.containsKey(group);
	}

	public PartRight getRight(final String group, final String key) {
		if (!this.right.containsKey(group)) {
			return null;
		}
		final Map<String, PartRight> rightGroup = this.right.get(group);
		if (!rightGroup.containsKey(key)) {
			return null;
		}
		return rightGroup.get(key);
	}

	public boolean hasRight(final String group, final String key, final Object value) {
		final Object data = getRight(group, key);
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
