package org.atriasoft.archidata.dataAccess;

import java.util.List;

public interface LazyGetter {
	void doRequest(List<LazyGetter> actions) throws Exception;
}
