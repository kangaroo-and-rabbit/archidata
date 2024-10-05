package org.kar.archidata.dataAccess;

import java.sql.PreparedStatement;
import java.util.List;

import org.bson.conversions.Bson;

public interface QueryItem {
	// For SQL mode query construction
	void generateQuery(StringBuilder query, String tableName);

	// For SQL mode query injection
	void injectQuery(PreparedStatement ps, CountInOut iii) throws Exception;

	// For No-SQL mode filter creation
	void generateFilter(List<Bson> filters);
}
