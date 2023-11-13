package org.kar.archidata.model;

import java.util.HashMap;
import java.util.Map;

public class UserByToken {
	public static final int TYPE_USER = -1;
	public static final int TYPE_APPLICATION = -2;
	// application internal management type: an application generic Id
	public Integer type = null;

	public Long id = null;
	public Long parentId = null; // FOr application, this is the id of the application, and of user token, this is the USERID
	public String name = null;
	// Right map
	public Map<String, Object> right = new HashMap<>();

	public boolean hasRight(final String key, final Object value) {
		if (!this.right.containsKey(key)) {
			return false;
		}
		final Object data = this.right.get(key);
		if (data instanceof final Boolean elem) {
			if (value instanceof final Boolean castVal) {
				if (elem == castVal) {
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
				if (elem == castVal) {
					return true;
				}
			}
			return false;
		}
		if (data instanceof final Double elem) {
			if (value instanceof final Double castVal) {
				if (elem == castVal) {
					return true;
				}
			}
			return false;
		}
		return false;
	}

}
