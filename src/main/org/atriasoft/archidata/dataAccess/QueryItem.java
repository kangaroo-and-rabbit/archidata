package org.atriasoft.archidata.dataAccess;

import org.bson.conversions.Bson;

public interface QueryItem {
	Bson getFilter();
}
