package org.atriasoft.archidata.migration;

import org.atriasoft.archidata.dataAccess.DBAccess;

public interface AsyncCall {
	void doRequest(DBAccess da) throws Exception;
}
