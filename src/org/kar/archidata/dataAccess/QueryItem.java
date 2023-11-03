package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;

public interface QueryItem {
	void generateQuerry(StringBuilder querry, String tableName);

	void injectQuerry(PreparedStatement ps, CountInOut iii) throws Exception;
}
