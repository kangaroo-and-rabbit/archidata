package org.atriasoft.archidata.dataAccess.options;

/**
 * Query option that limits the number of results returned from a database query.
 *
 * <p>The value must be in {@code 1..}{@link #getMaxValue()}: MongoDB reads {@code 0} as "no limit" and a
 * negative value as "a single batch", so both are rejected instead of silently returning everything. The
 * maximum ({@value #DEFAULT_MAX_VALUE} by default) is set with {@link #setMaxValue(long)}.
 */
public class Limit extends QueryOption {
	/** Default maximum accepted by the constructor. */
	public static final long DEFAULT_MAX_VALUE = 10_000L;

	private static volatile long maxValue = DEFAULT_MAX_VALUE;

	/** The maximum number of results to return. */
	protected final long limit;

	/**
	 * Constructs a Limit option with the specified maximum result count.
	 *
	 * @param limit the maximum number of results to return, in {@code 1..}{@link #getMaxValue()}
	 * @throws IllegalArgumentException if {@code limit} is not strictly positive or exceeds {@link #getMaxValue()}
	 */
	public Limit(final long limit) {
		if (limit <= 0) {
			throw new IllegalArgumentException("Limit value must be > 0, got: " + limit);
		}
		final long max = maxValue;
		if (limit > max) {
			throw new IllegalArgumentException("Limit value must be <= " + max + ", got: " + limit);
		}
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

	/**
	 * Returns the largest value accepted by the constructor.
	 *
	 * @return the current maximum limit
	 */
	public static long getMaxValue() {
		return maxValue;
	}

	/**
	 * Sets the largest value accepted by the constructor (application-wide, read at each construction).
	 *
	 * @param max the new maximum, in {@code 1..Integer.MAX_VALUE} (the driver takes an {@code int})
	 * @throws IllegalArgumentException if {@code max} is out of range
	 */
	public static void setMaxValue(final long max) {
		if (max <= 0 || max > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Limit maximum must be in 1.." + Integer.MAX_VALUE + ", got: " + max);
		}
		maxValue = max;
	}
}
