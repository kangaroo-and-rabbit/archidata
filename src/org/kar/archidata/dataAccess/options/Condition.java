package org.kar.archidata.dataAccess.options;

import java.sql.PreparedStatement;

import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.QueryItem;
import org.kar.archidata.dataAccess.QueryOption;
import org.kar.archidata.dataAccess.QueryOptions;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public class Condition extends QueryOption {
	public final QueryItem condition;

	public Condition(final QueryItem items) {
		this.condition = items;
	}

	public Condition() {
		this.condition = null;
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

	public void whereAppendQuery(final StringBuilder query, final String tableName, final QueryOptions options, final String deletedFieldName) {
		boolean exclude_deleted = true;
		if (options != null) {
			exclude_deleted = !options.exist(AccessDeletedItems.class);
		}
		// Check if we have a condition to generate
		if (this.condition == null) {
			if (exclude_deleted && deletedFieldName != null) {
				query.append(" WHERE ");
				query.append(tableName);
				query.append(".");
				query.append(deletedFieldName);
				query.append(" = false \n");
			}
			return;
		}
		query.append(" WHERE (");
		this.condition.generateQuerry(query, tableName);
		query.append(") ");
		if (exclude_deleted && deletedFieldName != null) {
			query.append("AND ");
			query.append(tableName);
			query.append(".");
			query.append(deletedFieldName);
			query.append(" = false ");
		}
		query.append("\n");
	}
}
