package org.kar.archidata.dataAccess.exportTools;

public class TableQueryTypes {

	public Class<?> type;
	public String name;
	public String title;

	public TableQueryTypes(final Class<?> type, final String name, final String title) {
		this.type = type;
		this.name = name;
		this.title = title;
	}

	public TableQueryTypes(final Class<?> type, final String name) {
		this.type = type;
		this.name = name;
		this.title = name;
	}
}
