package org.atriasoft.archidata.dataAccess.options;

/**
 * Query option that renames a column in the result mapping.
 *
 * <p>Used to map a database column name to a different field name during data access operations.
 */
public class OptionRenameColumn extends QueryOption {
	/** The original column name. */
	public final String columnName;
	/**
	 * The new column name to use.
	 * @deprecated upper-case field name: read it through {@link #getNewName()}; the field becomes private.
	 */
	@Deprecated(since = "0.55.0", forRemoval = true)
	public final String ColumnNewName;

	/**
	 * Constructs an option to rename a column.
	 *
	 * @param name the original column name
	 * @param newName the new column name
	 */
	public OptionRenameColumn(final String name, final String newName) {
		this.columnName = name;
		this.ColumnNewName = newName;
	}

	/**
	 * Returns the new column name to use.
	 *
	 * @return the new column name
	 */
	public String getNewName() {
		return this.ColumnNewName;
	}

}
