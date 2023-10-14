package org.kar.archidata.sqlWrapper;

import java.sql.PreparedStatement;

public interface QuerryItem {
	void generateQuerry(StringBuilder querry, String tableName);
	
	int injectQuerry(PreparedStatement ps, int iii) throws Exception;
}
