package org.kar.archidata.migration;

import org.kar.archidata.dataAccess.DBAccess;

public interface AsyncCall {
	void doRequest(DBAccess da) throws Exception;
}
