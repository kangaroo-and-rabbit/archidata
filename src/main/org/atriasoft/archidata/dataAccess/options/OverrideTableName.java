package org.atriasoft.archidata.dataAccess.options;

/** Option that permit to access to a table structure with an other name that is define in the structure. Note: Internal use for link tables (see:
 * org.atriasoft.archidata.dataAccess.addOn.model.LinkTable). */
public class OverrideTableName extends QueryOption {
	private final String name;

	/**
	 * Constructs an option to override the table name.
	 *
	 * @param name the alternative table name to use
	 */
	public OverrideTableName(final String name) {
		this.name = name;
	}

	/**
	 * Returns the overridden table name.
	 *
	 * @return the alternative table name
	 */
	public String getName() {
		return this.name;
	}
}
