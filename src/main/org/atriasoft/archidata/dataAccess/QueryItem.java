package org.atriasoft.archidata.dataAccess;

import java.util.List;

import org.bson.conversions.Bson;

public interface QueryItem {
	// For No-SQL mode filter creation
	void generateFilter(List<Bson> filters);
}
