package org.atriasoft.archidata.migration;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;

public interface AsyncCall {
	void doRequest(DBAccessMongo da) throws Exception;
}
