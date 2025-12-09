package org.atriasoft.archidata.dataAccess.options;

public class Limit extends QueryOption {
	protected final long limit;

	public Limit(final long limit) {
		this.limit = limit;
	}

	public void generateQuery(final StringBuilder query, final String tableName) {
		query.append(" LIMIT ? \n");
	}

	public long getValue() {
		return this.limit;
	}
}
