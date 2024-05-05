package org.kar.archidata.dataAccess.options;

/** Option that permit to access to a table structure with an other name that is define in the structure. Note: Internal use for link tables (see:
 * org.kar.archidata.dataAccess.addOn.model.LinkTable). */
public class OverrideTableName extends QueryOption {
	private final String name;

	public OverrideTableName(final String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
}
