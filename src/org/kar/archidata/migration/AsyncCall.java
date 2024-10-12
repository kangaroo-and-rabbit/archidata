package org.kar.archidata.migration;

import org.kar.archidata.dataAccess.DataAccess;

public interface AsyncCall {
	void doRequest(DataAccess da) throws Exception;
}
