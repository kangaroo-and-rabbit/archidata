package org.atriasoft.archidata.dataAccess.exportTools;

import java.util.ArrayList;
import java.util.List;

public class TableQuery {

	public final List<TableQueryTypes> headers;
	public final List<List<Object>> values = new ArrayList<>();

	public TableQuery(final List<TableQueryTypes> headers) {
		this.headers = headers;
	}
}
