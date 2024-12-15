package org.kar.archidata.dataAccess.options;

import java.sql.PreparedStatement;

import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.DBAccessSQL;

public class Limit extends QueryOption {
	protected final long limit;

	public Limit(final long limit) {
		this.limit = limit;
	}

	public void generateQuery(final StringBuilder query, final String tableName) {
		query.append(" LIMIT ? \n");
	}

	public void injectQuery(final DBAccessSQL ioDb, final PreparedStatement ps, final CountInOut iii) throws Exception {
		ioDb.addElement(ps, this.limit, iii);
		iii.inc();
	}

	public long getValue() {
		return this.limit;
	}
}
