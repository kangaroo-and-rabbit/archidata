package org.kar.archidata.dataAccess;

import java.sql.ResultSet;

public interface RetreiveFromDB {
	void doRequest(final ResultSet rs, Object obj) throws Exception;
}
