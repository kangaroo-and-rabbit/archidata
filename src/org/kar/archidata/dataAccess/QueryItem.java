package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;

public interface QueryItem {
	void generateQuerry(StringBuilder querry, String tableName);
	
	int injectQuerry(PreparedStatement ps, int iii) throws Exception;
}
