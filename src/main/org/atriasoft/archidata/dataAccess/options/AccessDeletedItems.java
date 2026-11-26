package org.atriasoft.archidata.dataAccess.options;

/** Includes the soft-deleted documents (those whose {@code @DataDeleted} field is {@code true}) in the
 * operation. By default a read, a count, an update, a delete or a restore never matches them.
 */
public class AccessDeletedItems extends QueryOption {
	/** Constructs an option to include deleted items in query results. */
	public AccessDeletedItems() {
		// default constructor
	}
}
