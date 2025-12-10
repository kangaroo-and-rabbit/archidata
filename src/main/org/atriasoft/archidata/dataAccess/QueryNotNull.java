package org.atriasoft.archidata.dataAccess;

public class QueryNotNull extends QueryExist {

	public QueryNotNull(final String key) {
		super(key, true);
	}
}
