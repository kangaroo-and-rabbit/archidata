package org.atriasoft.archidata.dataAccess.options;

/**
 * Query option that limits the number of results returned from a database query.
 */
public class Limit extends QueryOption {
	/** The maximum number of results to return. */
	protected final long limit;

	/**
	 * Constructs a Limit option with the specified maximum result count.
	 *
	 * @param limit the maximum number of results to return
	 */
	public Limit(final long limit) {
		this.limit = limit;
	}

	/**
	 * Returns the limit value.
	 *
	 * @return the maximum number of results
	 */
	public long getValue() {
		return this.limit;
	}
}
