package org.atriasoft.archidata.dataAccess;

/** Java does not permit to set return data (eg: integer) in the function parameter. This class permit to update a value as in/out function parameters. */
public class CountInOut {
	// internal value of the stream
	public int value = 0;

	/** Default constructor */
	public CountInOut() {}

	/** Constructor with the initial value.
	 * @param i Initial Value */
	public CountInOut(final int i) {
		this.value = i;
	}

	/** Increment by one the value. */
	public void inc() {
		this.value++;
	}
}
