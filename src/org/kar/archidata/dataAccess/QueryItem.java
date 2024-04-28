package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;

public interface QueryItem {
	void generateQuery(StringBuilder query, String tableName);

	void injectQuery(PreparedStatement ps, CountInOut iii) throws Exception;
}
