package org.atriasoft.archidata.dataAccess;

import org.bson.conversions.Bson;

// Note the query Item is deprecated soon, please use Filter.xxx() instead
public interface QueryItem {
	Bson getFilter();
}
