package org.atriasoft.archidata.dataAccess;

import java.util.List;

import org.bson.conversions.Bson;

public interface QueryItem {
	// For SQL mode query construction
	void generateQuery(StringBuilder query, String tableName);

	// For No-SQL mode filter creation
	void generateFilter(List<Bson> filters);
}
