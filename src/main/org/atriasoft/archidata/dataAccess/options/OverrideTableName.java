package org.atriasoft.archidata.dataAccess.options;

/** Reads and writes the collection named by this option instead of the one of the entity class (its
 * {@code @Collection} name, or its simple class name by default). */
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
