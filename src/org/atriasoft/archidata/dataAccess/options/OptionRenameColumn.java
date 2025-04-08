package org.atriasoft.archidata.dataAccess.options;

public class OptionRenameColumn extends QueryOption {
	public final String columnName;
	public final String colomnNewName;

	public OptionRenameColumn(final String name, final String newName) {
		this.columnName = name;
		this.colomnNewName = newName;
	}

}
