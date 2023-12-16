package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;

public interface QueryItem {
	void generateQuerry(StringBuilder query, String tableName);

	void injectQuerry(PreparedStatement ps, CountInOut iii) throws Exception;
}
