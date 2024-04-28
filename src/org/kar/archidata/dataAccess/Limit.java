package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;

public class Limit extends QueryOption {
	protected final long limit;

	public Limit(final long limit) {
		this.limit = limit;
	}

	public void generateQuery(final StringBuilder query, final String tableName) {
		query.append(" LIMIT ? \n");
	}

	public void injectQuery(final PreparedStatement ps, final CountInOut iii) throws Exception {
		DataAccess.addElement(ps, this.limit, iii);
		iii.inc();
	}
}
