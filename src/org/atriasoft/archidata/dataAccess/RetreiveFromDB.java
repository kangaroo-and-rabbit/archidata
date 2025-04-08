package org.atriasoft.archidata.dataAccess;

import java.sql.ResultSet;

public interface RetreiveFromDB {
	void doRequest(final ResultSet rs, Object obj) throws Exception;
}
