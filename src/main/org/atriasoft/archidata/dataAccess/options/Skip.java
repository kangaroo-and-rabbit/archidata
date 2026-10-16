package org.atriasoft.archidata.dataAccess.options;

/**
 * Query option that skips a number of results before returning them from a database query.
 * Combined with {@link Limit}, this is the building block of offset-based pagination.
 */
public class Skip extends QueryOption {
	/** Largest accepted value: the driver takes an {@code int}, a larger value used to be truncated. */
	public static final long MAX_VALUE = Integer.MAX_VALUE;

	/** The number of results to skip. */
	protected final long skip;

	/**
	 * Constructs a Skip option with the specified number of results to skip.
	 *
	 * @param skip the number of results to skip (must be in {@code 0..}{@link #MAX_VALUE})
	 * @throws IllegalArgumentException if {@code skip} is negative or exceeds {@link #MAX_VALUE}
	 */
	public Skip(final long skip) {
		if (skip < 0) {
			throw new IllegalArgumentException("Skip value must be >= 0, got: " + skip);
		}
		if (skip > MAX_VALUE) {
			throw new IllegalArgumentException("Skip value must be <= " + MAX_VALUE + ", got: " + skip);
		}
		this.skip = skip;
	}

	/**
	 * Returns the skip value.
	 *
	 * @return the number of results to skip
	 */
	public long getValue() {
		return this.skip;
	}
}
