package org.atriasoft.archidata.dataAccess.options;

/** Internal option that permit to transmit the Key when updating the ManyToMany values (first step). */
public class TransmitKey extends QueryOption {
	private final Object key;

	/**
	 * Constructs a TransmitKey with the specified key value.
	 *
	 * @param key the key to transmit during ManyToMany update operations
	 */
	public TransmitKey(final Object key) {
		this.key = key;
	}

	/**
	 * Returns the transmitted key.
	 *
	 * @return the key object
	 */
	public Object getKey() {
		return this.key;
	}
}
