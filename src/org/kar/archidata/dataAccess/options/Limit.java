package org.kar.archidata.dataAccess.options;

import org.kar.archidata.dataAccess.QueryOption;

/** Option that permit to access to a table structure with an other name that is define in the structure. Note: Internal use for link tables (see:
 * org.kar.archidata.dataAccess.addOn.model.LinkTable). */
public class Limit extends QueryOption {
	private final int limit;

	public Limit(final int limit) {
		this.limit = limit;
	}

	public int getLimit() {
		return this.limit;
	}
}
