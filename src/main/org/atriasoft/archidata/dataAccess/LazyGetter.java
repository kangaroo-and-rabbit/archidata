package org.atriasoft.archidata.dataAccess;

import java.util.List;

/**
 * Interface for deferred data retrieval operations.
 *
 * <p>Implementations collect lazy-loading requests that are batched and executed together
 * to reduce the number of database round-trips.
 */
public interface LazyGetter {
	/**
	 * Executes this lazy-loading request and may add follow-up actions to the list.
	 *
	 * @param actions the mutable list of pending lazy-loading actions to which follow-up requests can be added
	 * @throws Exception if the data retrieval fails
	 */
	void doRequest(List<LazyGetter> actions) throws Exception;
}
