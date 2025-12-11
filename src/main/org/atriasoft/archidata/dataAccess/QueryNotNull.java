package org.atriasoft.archidata.dataAccess;

// Note the query Item is deprecated soon, please use Filter.xxx() instead
public class QueryNotNull extends QueryExist {

	public QueryNotNull(final String key) {
		super(key, true);
	}
}
