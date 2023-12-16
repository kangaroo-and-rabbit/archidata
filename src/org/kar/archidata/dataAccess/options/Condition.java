package org.kar.archidata.dataAccess.options;

import java.sql.PreparedStatement;

import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.QueryItem;
import org.kar.archidata.dataAccess.QueryOption;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public class Condition extends QueryOption {
	public final QueryItem condition;

	public Condition(final QueryItem items) {
		this.condition = items;
	}

	public void generateQuerry(final StringBuilder query, final String tableName) {
		if (this.condition != null) {
			this.condition.generateQuerry(query, tableName);
		}
	}

	public void injectQuerry(final PreparedStatement ps, final CountInOut iii) throws Exception {
		if (this.condition != null) {
			this.condition.injectQuerry(ps, iii);
		}
	}
}
